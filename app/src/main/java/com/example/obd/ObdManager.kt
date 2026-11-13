package com.example.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.model.BluetoothDeviceItem
import com.example.model.CustomPidQuery
import com.example.model.DiagnosticsState
import com.example.model.DriveMode
import com.example.model.DtcKind
import com.example.model.dtcEntryOf
import com.example.model.HybridEnergyFlowState
import com.example.model.SensorRegistry
import com.example.model.ObdConnectionState
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Cliente ELM327 (Bluetooth Classic / SPP).
 *
 * Pontos principais:
 *  - Toda resposta é lida até o prompt '>' do ELM327 (com timeout), evitando respostas trocadas entre PIDs.
 *  - O init só marca CONECTADO depois que a ECU responde (0100).
 *  - Consulta só os PIDs que a ECU declara suportar (bitmaps 0100/0120/0140/0160).
 *  - PIDs rápidos (velocidade, RPM, vazão, TPS) a cada ciclo; os lentos giram 1 por ciclo.
 *  - Perda de enlace => reconexão automática (até MAX_RECONNECTS) e depois ERROR.
 *  - Valores indisponíveis ficam NaN (a UI mostra "--"), nunca valores inventados.
 */
class ObdManager {

    companion object {
        private const val TAG = "CibaAuto_OBD"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private const val MAX_RECONNECTS = 3
        private const val MAX_ECU_WAITS = 4          // tentativas extras esperando a ECU (ignição desligada)
        private const val MAX_SILENT_REPLIES = 8      // respostas vazias seguidas => enlace caiu
        private const val PID_FAILS_TO_DISABLE = 8

        // Sondagem de PIDs que a ECU não anuncia na lista de suportados (ver probeUndeclaredPid)
        private const val PROBE_EVERY_N_CYCLES = 3   // 1 consulta de sondagem a cada 3 ciclos
        private const val PROBE_FAILS_TO_STOP = 3    // desiste do PID após 3 tentativas sem resposta

        private const val PID_STATUS = 0x01
        private const val PID_ENGINE_LOAD = 0x04
        private const val PID_COOLANT = 0x05
        private const val PID_MAP = 0x0B
        private const val PID_RPM = 0x0C
        private const val PID_SPEED = 0x0D
        private const val PID_MAF = 0x10
        private const val PID_TPS = 0x11
        private const val PID_FUEL_LEVEL = 0x2F
        // Corolla Hybrid: fuel level via Combination Meter (header 7C0, Mode 21 PID 29 or Mode 22 PID 1022)
        private const val CUSTOM_FUEL_HEADER = "7C0"
        private const val CUSTOM_FUEL_PID = "2129"
        private const val CUSTOM_FUEL_RESPONSE = "6129"
        private const val CUSTOM_FUEL_PID_ALT = "221022"
        private const val CUSTOM_FUEL_RESPONSE_ALT = "621022" 
        private const val PID_BARO = 0x33
        private const val PID_ETHANOL = 0x52
        private const val PID_HYBRID_BATT = 0x5B
        // Híbridos Toyota: ECU da bateria HV responde no header 7E2 (resposta 7EA)
        private const val HV_HEADER = "7E2"
        private const val HV_FAILS_TO_STOP = 6
        private const val PID_OIL_TEMP = 0x5C
        private const val PID_FUEL_RATE = 0x5E

        private val CORE_PIDS = setOf(PID_SPEED, PID_RPM)

        // Combustível brasileiro: gasolina comum ~E27
        private const val DEFAULT_ETHANOL_FRACTION = 0.27f
    }

    private val _connectionState = MutableStateFlow(ObdConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDeviceItem?>(null)
    val connectedDevice: StateFlow<BluetoothDeviceItem?> = _connectedDevice.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    // ---- Diagnóstico (códigos de falha) ----
    private val _diagnostics = MutableStateFlow(DiagnosticsState())
    val diagnostics: StateFlow<DiagnosticsState> = _diagnostics.asStateFlow()

    private enum class DiagRequest { SCAN, CLEAR }
    private val diagRequests = Channel<DiagRequest>(Channel.BUFFERED)
    @Volatile private var isCanProtocol = true

    /** Pede a leitura dos códigos (executada pelo loop de leitura, sem conflitar com os PIDs). */
    fun requestScan() {
        if (_connectionState.value == ObdConnectionState.CONNECTED) {
            _diagnostics.update { it.copy(isScanning = true, message = null) }
            diagRequests.trySend(DiagRequest.SCAN)
        } else {
            _diagnostics.update { it.copy(message = "Conecte o adaptador OBD para ler os códigos.") }
        }
    }

    /** Pede o apagamento dos códigos (modo 04). A UI deve confirmar com o usuário antes. */
    fun requestClear() {
        if (_connectionState.value == ObdConnectionState.CONNECTED) {
            _diagnostics.update { it.copy(isScanning = true, message = null) }
            diagRequests.trySend(DiagRequest.CLEAR)
        } else {
            _diagnostics.update { it.copy(message = "Conecte o adaptador OBD para apagar os códigos.") }
        }
    }

    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var inputStream: InputStream? = null
    @Volatile private var outputStream: OutputStream? = null
    @Volatile private var pollJob: Job? = null
    @Volatile private var generation = 0
    @Volatile private var profile: VehicleProfile? = null

    private var silentReplies = 0
    private var lastSeenDtcCount = -1
    private var verboseLog = true
    private val pidFailures = mutableMapOf<Int, Int>()
    private val disabledPids = mutableSetOf<Int>()
    // Sondagem de PIDs não anunciados: contadores próprios (não mexem em pidFailures/disabledPids)
    private val probeFails = mutableMapOf<Int, Int>()
    private val probeConfirmed = mutableSetOf<Int>()

    private class LinkLostException(msg: String) : IOException(msg)

    private sealed class InitResult {
        data class Ready(val supported: Set<Int>) : InitResult()
        data class NoEcu(val reason: String) : InitResult()
    }

    fun addLog(msg: String) {
        Log.d(TAG, msg)
        _diagnosticLogs.update { old ->
            val list = if (old.size >= 60) old.drop(old.size - 59) else old
            list + msg
        }
    }

    /** Permite trocar o perfil do veículo sem reconectar. */
    fun updateProfile(newProfile: VehicleProfile) {
        profile = newProfile
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceItem> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.map {
                BluetoothDeviceItem(
                    name = it.name ?: "Dispositivo Desconhecido",
                    address = it.address,
                    isBonded = true,
                    isConnected = it.address == _connectedDevice.value?.address
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing paired devices: ${e.message}")
            emptyList()
        }
    }

    // ------------------------------------------------------------------------------------
    // Conexão
    // ------------------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        deviceItem: BluetoothDeviceItem,
        scope: CoroutineScope,
        profile: VehicleProfile,
        onTelemetryUpdate: (TelemetryData) -> Unit
    ) {
        disconnect()
        this.profile = profile
        _diagnostics.value = DiagnosticsState()
        while (diagRequests.tryReceive().isSuccess) { /* descarta pedidos antigos */ }
        val gen = generation
        _connectionState.value = ObdConnectionState.CONNECTING
        addLog("Conectando a ${deviceItem.name} (${deviceItem.address})...")

        pollJob = scope.launch(Dispatchers.IO) {
            var attempts = 0
            try {
                while (isActive && gen == generation) {
                    var linkLost = false
                    try {
                        openSocket(deviceItem)
                        if (gen != generation) return@launch

                        // Adaptador Bluetooth já conectado (como no projeto antigo); agora espera a ECU responder
                        _connectedDevice.value = deviceItem.copy(isConnected = true)
                        var init = initElm327()
                        var ecuTries = 0
                        while (init is InitResult.NoEcu && ecuTries < MAX_ECU_WAITS && isActive && gen == generation) {
                            ecuTries++
                            addLog("Adaptador conectado, mas a ECU não respondeu (${init.reason}).")
                            addLog("Ligue a ignição (no híbrido, aperte Power até READY). Nova tentativa $ecuTries/$MAX_ECU_WAITS...")
                            delay(3000)
                            init = initElm327()
                        }
                        if (gen != generation) return@launch
                        when (val result = init) {
                            is InitResult.NoEcu -> {
                                addLog("ECU não respondeu: ${result.reason}")
                                _connectedDevice.value = null
                                _connectionState.value = ObdConnectionState.ERROR
                                return@launch
                            }
                            is InitResult.Ready -> {
                                _connectionState.value = ObdConnectionState.CONNECTED
                                addLog("ECU conectada. PIDs suportados: ${result.supported.size}")
                                attempts = 0
                                pollLoop(result.supported, gen, onTelemetryUpdate)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SecurityException) {
                        addLog("Permissão Bluetooth negada: conceda 'Dispositivos próximos' ao app.")
                        if (gen == generation) _connectionState.value = ObdConnectionState.ERROR
                        return@launch
                    } catch (e: Exception) {
                        if (gen != generation) return@launch
                        Log.e(TAG, "Falha/perda de conexão: ${e.message}", e)
                        addLog("Falha: ${e.message}")
                        linkLost = true
                    }

                    if (gen != generation) return@launch
                    closeStreams()
                    if (!linkLost) return@launch

                    attempts++
                    if (attempts > MAX_RECONNECTS) {
                        addLog("Não foi possível reconectar. Verifique o adaptador.")
                        _connectionState.value = ObdConnectionState.ERROR
                        return@launch
                    }
                    _connectionState.value = ObdConnectionState.CONNECTING
                    addLog("Reconectando ($attempts/$MAX_RECONNECTS)...")
                    delay(2000L * attempts)
                }
            } finally {
                if (gen == generation) closeStreams()
            }
        }
    }

    fun disconnect() {
        generation++
        pollJob?.cancel()
        pollJob = null
        closeStreams()
        _connectionState.value = ObdConnectionState.DISCONNECTED
        _connectedDevice.value = null
        _diagnostics.update { it.copy(isScanning = false) }
        addLog("Desconectado.")
    }

    private fun closeStreams() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    /** Abre o socket SPP; se falhar, tenta o canal inseguro e, por fim, RFCOMM canal 1 (clones). */
    @SuppressLint("MissingPermission")
    private fun openSocket(deviceItem: BluetoothDeviceItem) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            throw IOException("Bluetooth não suportado ou desligado")
        }
        // No Android 12+ cancelDiscovery() exige BLUETOOTH_SCAN (permissão removida do app).
        // Não há busca ativa, então ignorar a falha é seguro; antes isso derrubava a conexão.
        try { adapter.cancelDiscovery() } catch (_: SecurityException) { }
        val device: BluetoothDevice = adapter.getRemoteDevice(deviceItem.address)

        val attempts: List<Pair<String, () -> BluetoothSocket>> = listOf(
            "SPP seguro" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            "SPP inseguro" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            "RFCOMM canal 1" to {
                device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
            }
        )

        var lastError: Exception? = null
        for ((name, factory) in attempts) {
            var attemptSocket: BluetoothSocket? = null
            try {
                val s = factory()
                attemptSocket = s
                s.connect()
                socket = s
                inputStream = s.inputStream
                outputStream = s.outputStream
                addLog("Bluetooth conectado ($name).")
                return
            } catch (e: SecurityException) {
                try { attemptSocket?.close() } catch (_: Exception) {}
                throw e
            } catch (e: Exception) {
                // Fecha o socket que falhou antes da próxima tentativa (senão a seguinte também falha)
                try { attemptSocket?.close() } catch (_: Exception) {}
                lastError = e
                addLog("$name falhou: ${e.message}")
            }
        }
        throw IOException(lastError?.message ?: "Não foi possível abrir o socket Bluetooth")
    }

    // ------------------------------------------------------------------------------------
    // Camada ELM327
    // ------------------------------------------------------------------------------------

    /** Envia o comando e lê até o prompt '>' (ou timeout). Lança IOException se o enlace falhou. */
    private suspend fun sendCommand(cmd: String, timeoutMs: Long = 2000, log: Boolean = verboseLog): String {
        val out = outputStream ?: throw IOException("Sem conexão")
        val inp = inputStream ?: throw IOException("Sem conexão")

        // Descarta resíduos de respostas anteriores
        var pending = inp.available()
        while (pending > 0) {
            inp.skip(pending.toLong())
            pending = inp.available()
        }

        out.write("$cmd\r".toByteArray(Charsets.US_ASCII))
        out.flush()

        val sb = StringBuilder()
        val buf = ByteArray(128)
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        while (System.nanoTime() < deadline) {
            val n = inp.available()
            if (n > 0) {
                val read = inp.read(buf, 0, minOf(n, buf.size))
                if (read < 0) throw IOException("Stream fechado")
                sb.append(String(buf, 0, read, Charsets.US_ASCII))
                if (sb.indexOf(">") >= 0) break
            } else {
                delay(6)
            }
        }

        val raw = sb.toString().replace(">", "")
        if (raw.isBlank()) {
            silentReplies++
            if (silentReplies >= MAX_SILENT_REPLIES) {
                throw LinkLostException("Adaptador parou de responder")
            }
        } else {
            silentReplies = 0
        }
        if (log) addLog("TX: $cmd -> RX: ${raw.replace('\r', ' ').replace('\n', ' ').trim()}")
        return raw
    }

    private suspend fun initElm327(): InitResult {
        silentReplies = 0
        lastSeenDtcCount = -1
        verboseLog = true
        pidFailures.clear()
        disabledPids.clear()
        probeFails.clear()
        probeConfirmed.clear()

        sendCommand("ATZ", 3500)
        delay(200)
        sendCommand("ATE0", 1000)
        sendCommand("ATL0", 1000)
        sendCommand("ATS0", 1000)
        sendCommand("ATH0", 1000)
        sendCommand("ATAT1", 1000)
        sendCommand("ATST64", 1000)
        sendCommand("ATSP0", 1000)

        var lastReply = ""
        var ecuOk = false
        for (attempt in 1..3) {
            // A primeira consulta dispara a busca automática de protocolo (pode levar vários segundos)
            lastReply = sendCommand("0100", 12000)
            if (dataBytes(lastReply, "4100") != null) {
                ecuOk = true
                break
            }
            addLog("Tentativa $attempt/3 sem resposta da ECU")
            delay(1500)
        }
        if (!ecuOk) {
            return InitResult.NoEcu(lastReply.replace('\r', ' ').trim().ifBlank { "sem resposta" })
        }

        isCanProtocol = DtcParser.isCanProtocol(sendCommand("ATDPN", 1000))
        val supported = readSupportedPids()

        if (profile?.hasTurbo == true && (supported.isEmpty() || PID_BARO in supported)) {
            // Pressão barométrica é lida uma vez; usada para converter MAP absoluto em boost
            readPid(PID_BARO)?.let { baroKpa = it[0].toFloat() }
        }
        verboseLog = false  // daqui em diante, só erros e resumo periódico
        return InitResult.Ready(supported)
    }

    private var baroKpa = 101f

    // ---- Sistema híbrido Toyota (ECU da bateria HV) ----
    @Volatile private var hvSocFails = 0
    @Volatile private var hvProbeDone = false

    /**
     * SOC da bateria HV: PID padrão 015B enviado direto, mesmo que a ECU não o anuncie na lista de
     * PIDs suportados (o Corolla híbrido responde mesmo assim). Fórmula SAE: A * 100 / 255.
     * Se a ECU não responder, desiste após algumas tentativas.
     */
    private suspend fun readToyotaHvSoc(): Float? {
        if (hvSocFails >= HV_FAILS_TO_STOP) return null
        return try {
            val bytes = dataBytes(sendCommand("015B", 1500), "415B")
            if (bytes != null && bytes.isNotEmpty()) {
                hvSocFails = 0
                (bytes[0] * 100f / 255f).coerceIn(0f, 100f)
            } else {
                hvSocFails++
                if (hvSocFails == HV_FAILS_TO_STOP) addLog("SOC híbrido (015B) sem resposta: leitura desativada nesta sessão")
                null
            }
        } catch (e: Exception) {
            hvSocFails++
            null
        }
    }

    /**
     * Teste único por sessão: envia consultas à ECU híbrida e grava as respostas CRUAS no log.
     * Serve para descobrir quais comandos o Corolla híbrido realmente responde.
     */
    private suspend fun probeToyotaHybrid() {
        if (hvProbeDone || !isCanProtocol) return
        hvProbeDone = true
        val was = verboseLog
        verboseLog = true
        addLog("=== TESTE HÍBRIDO TOYOTA: início (leva ~30 s) ===")
        try {
            sendCommand("ATH1", 800)   // mostra QUAL ECU respondeu (7E8 motor, 7EA híbrida...)
            sendCommand("ATS1", 800)
            addLog("-- endereçamento funcional (7DF): tipo de combustível / etanol --")
            for (c in listOf("0151", "0152")) sendCommand(c, 2000, true)
            val probes = listOf(
                "0100", "015B", "2101", "2102", "2103", "2104", "2105",
                "2106", "2107", "2108", "2109", "2181", "21CE", "2201", "22F190"
            )
            for (header in listOf(HV_HEADER, "7E3", "7E0")) {
                sendCommand("ATSH$header", 800)
                addLog("-- header $header --")
                for (c in probes) sendCommand(c, 2000, true)
            }
        } catch (e: Exception) {
            addLog("Teste híbrido interrompido: ${e.message}")
        } finally {
            try {
                sendCommand("ATSH7DF", 800)
                sendCommand("ATAR", 800)
                sendCommand("ATH0", 800)
                sendCommand("ATS0", 800)
            } catch (_: Exception) {}
            addLog("=== TESTE HÍBRIDO TOYOTA: fim ===")
            verboseLog = was
        }
    }

    /** PIDs de sensores adicionais que estão em manômetros: lidos com prioridade (metade dos ciclos). */
    @Volatile
    private var priorityPids: Set<Int> = emptySet()

    fun setPriorityPids(pids: Set<Int>) {
        priorityPids = pids
    }

    /** Lê os bitmaps de PIDs suportados (0100, 0120, 0140, 0160). */
    private suspend fun readSupportedPids(): Set<Int> {
        val set = mutableSetOf<Int>()
        var base = 0x00
        while (base <= 0x60) {
            val hex = "%02X".format(base)
            val bytes = dataBytes(sendCommand("01$hex", 3000), "41$hex") ?: break
            if (bytes.size < 4) break
            for (i in 0 until 32) {
                if (bytes[i / 8] and (0x80 shr (i % 8)) != 0) set.add(base + i + 1)
            }
            if ((base + 0x20) !in set) break
            base += 0x20
        }
        return set
    }

    private suspend fun readPid(pid: Int): IntArray? {
        if (pid in disabledPids) return null
        val hex = "%02X".format(pid)
        val bytes = dataBytes(sendCommand("01$hex", 1500), "41$hex")
        if (bytes == null) {
            val fails = (pidFailures[pid] ?: 0) + 1
            pidFailures[pid] = fails
            if (fails >= PID_FAILS_TO_DISABLE && pid !in CORE_PIDS) {
                disabledPids.add(pid)
                addLog("PID 01$hex sem resposta: desativado nesta sessão")
            }
        } else {
            pidFailures[pid] = 0
        }
        return bytes
    }

    /**
     * Sondagem de um PID Mode 01 que a ECU NÃO anuncia na lista de suportados. Vários módulos Toyota
     * respondem a PIDs fora da lista (o 0152 e o 015B já funcionam assim neste app). Usa a mesma
     * fórmula do sensor ([decode]); contadores próprios, sem alterar pidFailures/disabledPids.
     * Se responder, o PID entra em [probeConfirmed] e passa a ser lido na rotina normal.
     * Se falhar [PROBE_FAILS_TO_STOP] vezes, o sensor segue "sem leitura".
     */
    private suspend fun probeUndeclaredPid(pid: Int, decode: (IntArray) -> Float?): Float? {
        val hex = "%02X".format(pid)
        val value = dataBytes(sendCommand("01$hex", 1500), "41$hex")?.let(decode)
        if (value != null) {
            probeConfirmed.add(pid)
            probeFails[pid] = 0
            addLog("PID 01$hex não anunciado pela ECU, mas respondeu: leitura ativada")
        } else {
            val fails = (probeFails[pid] ?: 0) + 1
            probeFails[pid] = fails
            if (fails >= PROBE_FAILS_TO_STOP) {
                addLog("PID 01$hex não anunciado e sem resposta ($fails tentativas): segue sem leitura")
            }
        }
        return value
    }

    private suspend fun readBatteryVoltage(): Float? {
        val res = sendCommand("ATRV", 1000)
        val m = Regex("(\\d{1,2}(?:\\.\\d+)?)\\s*V", RegexOption.IGNORE_CASE).find(res)
        return m?.groupValues?.get(1)?.toFloatOrNull()
    }

    private suspend fun readCustomSoc(q: CustomPidQuery): Float? {
        return try {
            sendCommand("ATSH${q.header}", 800)
            val res = sendCommand(q.request, 2500)
            val bytes = dataBytes(res, q.responsePrefix)
            bytes?.getOrNull(q.byteIndex)?.let { (it * q.multiplier / q.divisor).coerceIn(0f, 100f) }
        } finally {
            sendCommand("ATSH7DF", 800) // volta ao endereçamento funcional (CAN 11 bits)
        }
    }

    /**
     * Detecta se o perfil do veículo corresponde a um híbrido Toyota (Corolla, Corolla Cross, RAV4, Prius, etc.).
     */
    private fun isToyotaHybrid(prof: VehicleProfile): Boolean {
        val isToyota = prof.brand.contains("Toyota", ignoreCase = true) ||
            prof.name.contains("Toyota", ignoreCase = true) ||
            prof.model.contains("Corolla", ignoreCase = true) ||
            prof.model.contains("Yaris", ignoreCase = true) ||
            prof.model.contains("Prius", ignoreCase = true) ||
            prof.model.contains("RAV4", ignoreCase = true)
        return isToyota && prof.isHybrid
    }

    /**
     * Leitura de combustível proprietária Toyota via Combination Meter (header 7C0):
     * 1. Troca o header para 7C0 (painel de instrumentos da Toyota)
     * 2. Tenta PID 2129 (Mode 21 PID 29 - padrão nos Corolla / RAV4 / Camry / Prius Hybrid)
     * 3. Se não responder, tenta Mode 22 PID 1022 (221022)
     * 4. Interpreta a resposta (byte A):
     *    - No Combination Meter da Toyota (PID 2129), o valor A vem em unidades de 0.5 Litro (A/2 L).
     *      Exemplo: A = 86 -> 43 L (100% de um tanque de 43 L).
     *    - Se A/2 exceder significativamente a capacidade do tanque, interpreta como escala normalizada 0..255 (A*100/255).
     * 5. Restaura sempre o header funcional padrão 7DF ao final.
     */
    private suspend fun readToyotaCustomFuel(prof: VehicleProfile): Float? {
        if (!isCanProtocol) return null
        return try {
            // 1. Troca o header para 7C0 (Combination Meter)
            sendCommand("ATSH$CUSTOM_FUEL_HEADER", 800)

            // 2. Tenta Mode 21 PID 29
            var res = sendCommand(CUSTOM_FUEL_PID, 2000)
            var bytes = dataBytes(res, CUSTOM_FUEL_RESPONSE)

            // 3. Fallback: Mode 22 PID 1022
            if (bytes == null || bytes.isEmpty()) {
                res = sendCommand(CUSTOM_FUEL_PID_ALT, 2000)
                bytes = dataBytes(res, CUSTOM_FUEL_RESPONSE_ALT)
            }

            if (bytes != null && bytes.isNotEmpty()) {
                val a = bytes[0]
                val tankCap = prof.tankCapacityLiters.toFloat().takeIf { it > 10f } ?: 43f

                val halfLiters = a * 0.5f
                val percent = if (halfLiters <= tankCap * 1.15f) {
                    // Valor em meio-litros (A/2 L)
                    (halfLiters / tankCap * 100f).coerceIn(0f, 100f)
                } else {
                    // Valor em escala percentual 0..255
                    (a * 100f / 255f).coerceIn(0f, 100f)
                }
                addLog("Leitura de combustível Toyota (header 7C0): raw=$a, ~${"%.1f".format(halfLiters)}L (${percent.toInt()}%)")
                percent
            } else {
                null
            }
        } catch (e: Exception) {
            addLog("Aviso: falha na leitura de combustível Toyota 7C0: ${e.message}")
            null
        } finally {
            try {
                sendCommand("ATSH7DF", 800) // restaura endereçamento funcional CAN
            } catch (_: Exception) {}
        }
    }

    /**
     * Extrai os bytes de dados que seguem [prefix] (ex.: "410D"). Considera cada linha da resposta
     * (com várias ECUs há várias linhas) e ignora ruído como SEARCHING... / NO DATA.
     */
    private fun dataBytes(res: String, prefix: String): IntArray? {
        for (line in res.split('\r', '\n')) {
            val l = line.replace(" ", "").uppercase()
            val idx = l.indexOf(prefix.uppercase())
            if (idx < 0) continue
            val hex = l.substring(idx + prefix.length).takeWhile { it in '0'..'9' || it in 'A'..'F' }
            if (hex.length < 2) continue
            val out = IntArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16) }
            return out
        }
        return null
    }

    // ------------------------------------------------------------------------------------
    // Loop de leitura
    // ------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------
    // Diagnóstico: modos 03 (armazenados), 07 (pendentes), 0A (permanentes), 04 (apagar)
    // ------------------------------------------------------------------------------------

    private fun looksLikeBusError(raw: String): Boolean {
        val u = raw.uppercase()
        return u.contains("UNABLE") || u.contains("BUS") || u.contains("ERROR") ||
            u.contains("STOPPED") || u.contains("?") || u.isBlank()
    }

    private suspend fun scanDtcs(clearedMessage: String? = null) {
        verboseLog = true
        try {
            val status = DtcParser.parseMonitorStatus(sendCommand("0101", 3000))
            val rawStored = sendCommand("03", 6000)
            val rawPending = sendCommand("07", 6000)
            val rawPermanent = sendCommand("0A", 6000)
            verboseLog = false

            val storedOk = DtcParser.hasValidResponse(rawStored, "43") || rawStored.uppercase().contains("NO DATA")
            if (!storedOk && looksLikeBusError(rawStored)) {
                _diagnostics.update {
                    it.copy(isScanning = false, message = "Falha ao ler os códigos. Confira a ignição e a conexão e tente de novo.")
                }
                return
            }

            val stored = DtcParser.decodeDtcs(rawStored, "43", isCanProtocol)
            val pending = DtcParser.decodeDtcs(rawPending, "47", isCanProtocol)
            val permanent = DtcParser.decodeDtcs(rawPermanent, "4A", isCanProtocol)

            _diagnostics.update {
                it.copy(
                    milOn = status?.first ?: it.milOn,
                    reportedCount = status?.second ?: it.reportedCount,
                    stored = stored.map { c -> dtcEntryOf(c, DtcKind.STORED) },
                    pending = pending.map { c -> dtcEntryOf(c, DtcKind.PENDING) },
                    permanent = permanent.map { c -> dtcEntryOf(c, DtcKind.PERMANENT) },
                    isScanning = false,
                    lastScanMs = System.currentTimeMillis(),
                    message = clearedMessage
                )
            }
            if (status != null) lastSeenDtcCount = status.second
            addLog("Diagnóstico: ${stored.size} armazenado(s), ${pending.size} pendente(s), ${permanent.size} permanente(s)")
        } catch (e: IOException) {
            verboseLog = false
            _diagnostics.update { it.copy(isScanning = false, message = "Conexão perdida durante a leitura.") }
            throw e
        }
    }

    private suspend fun clearDtcs() {
        verboseLog = true
        try {
            val raw = sendCommand("04", 6000)
            verboseLog = false
            if (DtcParser.hasValidResponse(raw, "44")) {
                scanDtcs(clearedMessage = "Códigos apagados. Se o problema persistir, a luz volta a acender.")
            } else {
                _diagnostics.update {
                    it.copy(
                        isScanning = false,
                        message = "A ECU não aceitou apagar os códigos. Deixe a ignição ligada (motor desligado) e tente de novo."
                    )
                }
            }
        } catch (e: IOException) {
            verboseLog = false
            _diagnostics.update { it.copy(isScanning = false, message = "Conexão perdida ao apagar os códigos.") }
            throw e
        }
    }

    private fun blankTelemetry() = TelemetryData(
        speedKmh = 0f, avgSpeedKmh = 0f, rpm = 0f,
        fuelLevelPercent = 0f, batteryVoltage = 0f,
        coolantTempC = Float.NaN, oilTempC = Float.NaN,
        turboPressureKpa = 0f, engineLoadPercent = 0f, throttlePercent = 0f,
        lateralGForce = 0f,
        instantConsumptionKmPerL = 0f, avgConsumptionKmPerL = 0f,
        autonomyKm = 0, elapsedTimeSeconds = 0L,
        isEngineRunning = false, isEvMode = false,
        ethanolPercent = Float.NaN,
        hybridBatterySocPercent = 0f, hybridSocAvailable = false,
        hybridBatteryVoltageVolts = 0f, hybridBatteryCurrentAmps = 0f,
        hybridBatteryTempC = 0f, icePowerKw = 0f,
        electricMotorPowerKw = 0f, generatorPowerKw = 0f,
        hybridDetailAvailable = false,
        ecuLinkOk = false, fuelLevelAvailable = false
    )

    private suspend fun CoroutineScope.pollLoop(
        supported: Set<Int>,
        gen: Int,
        onTelemetryUpdate: (TelemetryData) -> Unit
    ) {
        fun usable(pid: Int) = pid !in disabledPids && (supported.isEmpty() || pid in supported)

        var t = blankTelemetry()
        val startNs = System.nanoTime()
        var lastSampleNs = startNs
        var speedTotal = 0f
        var speedCount = 0
        var distanceKm = 0.0
        var fuelUsedL = 0.0
        var instantEma = 0f
        var cycle = 0
        // Confiabilidade: instante da última resposta da ECU e das leituras lentas
        val linkTimeoutNs = 4_000_000_000L
        val slowStaleNs = 20_000_000_000L
        var lastCoreOkNs = startNs
        var lastLinkOk = true
        val slowOkNs = HashMap<Int, Long>()
        // Sensores adicionais (SensorRegistry): 1 por ciclo, só os que a ECU declarou suportar
        val extraStaleNs = 40_000_000_000L
        val extraValues = HashMap<String, Float>()
        val extraOkNs = HashMap<String, Long>()
        var extraIdx = 0
        var priorityIdx = 0
        var probeIdx = 0
        var lastSummaryNs = startNs
        var cyclesSinceSummary = 0

        val supportedExtras = SensorRegistry.standard.count { usable(it.pid) }
        addLog("Sensores adicionais suportados pela ECU: $supportedExtras de ${SensorRegistry.standard.size}")
        profile?.let { if (isToyotaHybrid(it)) probeToyotaHybrid() }

        while (isActive && gen == generation) {
            val prof = profile ?: break

            // ---------- Pedidos de diagnóstico (leitura/apagamento de códigos) ----------
            while (true) {
                val req = diagRequests.tryReceive().getOrNull() ?: break
                when (req) {
                    DiagRequest.SCAN -> scanDtcs()
                    DiagRequest.CLEAR -> clearDtcs()
                }
            }

            // ---------- PIDs rápidos ----------
            val speedBytes = readPid(PID_SPEED)?.takeIf { it.isNotEmpty() }
            val rpmBytes = readPid(PID_RPM)?.takeIf { it.size >= 2 }
            if (speedBytes != null || rpmBytes != null) lastCoreOkNs = System.nanoTime()
            val linkOk = System.nanoTime() - lastCoreOkNs < linkTimeoutNs
            if (linkOk != lastLinkOk) {
                lastLinkOk = linkOk
                addLog(if (linkOk) "ECU voltou a responder" else "ECU sem resposta: leituras marcadas como indisponíveis")
            }
            // Sem resposta da ECU não se reaproveita o valor antigo como se fosse atual
            val speed = if (!linkOk) 0f else (speedBytes?.get(0)?.toFloat() ?: t.speedKmh)
            val rpm = if (!linkOk) 0f else (rpmBytes?.let { (it[0] * 256 + it[1]) / 4f } ?: t.rpm)
            val tps = if (!linkOk) 0f else if (usable(PID_TPS)) {
                readPid(PID_TPS)?.let { it[0] * 100f / 255f } ?: t.throttlePercent
            } else t.throttlePercent

            // Vazão de combustível (L/h): 015E direto, ou calculada pelo MAF (0110)
            val ethFrac = if (!t.ethanolPercent.isNaN()) t.ethanolPercent / 100f else DEFAULT_ETHANOL_FRACTION
            var realFlowLph: Float? = null
            val hasFlowSource = usable(PID_FUEL_RATE) || usable(PID_MAF)
            if (usable(PID_FUEL_RATE)) {
                readPid(PID_FUEL_RATE)?.takeIf { it.size >= 2 }?.let {
                    realFlowLph = (it[0] * 256 + it[1]) / 20f
                }
            } else if (usable(PID_MAF)) {
                readPid(PID_MAF)?.takeIf { it.size >= 2 }?.let {
                    val mafGs = (it[0] * 256 + it[1]) / 100f
                    val afr = 14.7f * (1f - ethFrac) + 9.0f * ethFrac
                    val densityGperL = 745f * (1f - ethFrac) + 789f * ethFrac
                    realFlowLph = mafGs * 3600f / (afr * densityGperL)
                }
            }

            // ---------- PIDs lentos (1 por ciclo, em rotação) ----------
            var coolant = t.coolantTempC
            var fuel = t.fuelLevelPercent
            var load = t.engineLoadPercent
            var batt = t.batteryVoltage
            var oil = t.oilTempC
            var ethanol = t.ethanolPercent
            var soc = if (t.hybridSocAvailable) t.hybridBatterySocPercent else Float.NaN
            var turbo = t.turboPressureKpa

            val slowTasks = buildList {
                add(PID_STATUS) // luz de injeção (MIL) + quantidade de códigos
                if (usable(PID_COOLANT)) add(PID_COOLANT)
                if (usable(PID_FUEL_LEVEL) || isToyotaHybrid(prof)) add(PID_FUEL_LEVEL)
                add(-1) // tensão da bateria 12 V (ATRV)
                if (usable(PID_ENGINE_LOAD)) add(PID_ENGINE_LOAD)
                if (usable(PID_OIL_TEMP) || PID_OIL_TEMP in probeConfirmed) add(PID_OIL_TEMP)
                if (PID_ETHANOL !in disabledPids) add(PID_ETHANOL) // tenta 0152 direto, mesmo fora da lista anunciada
                if (usable(PID_HYBRID_BATT) || prof.socQuery != null ||
                    (prof.isHybrid && hvSocFails < HV_FAILS_TO_STOP)) add(PID_HYBRID_BATT)
                if (prof.hasTurbo && usable(PID_MAP)) add(PID_MAP)
            }
            if (slowTasks.isNotEmpty()) {
                when (val task = slowTasks[cycle % slowTasks.size]) {
                    PID_STATUS -> readPid(task)?.let { b ->
                        val mil = (b[0] and 0x80) != 0
                        val count = b[0] and 0x7F
                        _diagnostics.update { it.copy(milOn = mil, reportedCount = count) }
                        // Mudou a quantidade de códigos: lê automaticamente para avisar o usuário
                        if (count != lastSeenDtcCount) {
                            val firstReadClean = lastSeenDtcCount < 0 && count == 0 && !mil
                            lastSeenDtcCount = count
                            if (!firstReadClean) diagRequests.trySend(DiagRequest.SCAN)
                        }
                    }
                    PID_COOLANT -> readPid(task)?.let { coolant = (it[0] - 40).toFloat(); slowOkNs[task] = System.nanoTime() }
                    PID_FUEL_LEVEL -> {
                        var f: Float? = null
                        if (isToyotaHybrid(prof)) {
                            // 1. Nos híbridos Toyota, o PID padrão 012F muitas vezes não responde ou retorna valor fixo.
                            // Lê prioritariamente o módulo do painel (header 7C0) com ATSH7C0 e PIDs 2129 / 221022.
                            f = readToyotaCustomFuel(prof)
                            // 2. Fallback caso a ECU do painel não responda
                            if (f == null && usable(PID_FUEL_LEVEL)) {
                                f = readPid(task)?.let { it[0] * 100f / 255f }
                            }
                        } else {
                            // Demais veículos: lê PID padrão 012F
                            if (usable(PID_FUEL_LEVEL)) {
                                f = readPid(task)?.let { it[0] * 100f / 255f }
                            }
                            // Fallback para modelos Toyota que também utilizam painel no header 7C0
                            if (f == null && isCanProtocol && prof.brand.contains("Toyota", ignoreCase = true)) {
                                f = readToyotaCustomFuel(prof)
                            }
                        }

                        if (f != null && !f.isNaN()) {
                            fuel = f.coerceIn(0f, 100f)
                            slowOkNs[task] = System.nanoTime()
                        }
                    }
                    -1 -> readBatteryVoltage()?.let { batt = it; slowOkNs[task] = System.nanoTime() }
                    PID_ENGINE_LOAD -> readPid(task)?.let { load = it[0] * 100f / 255f; slowOkNs[task] = System.nanoTime() }
                    PID_OIL_TEMP -> readPid(task)?.let { oil = (it[0] - 40).toFloat(); slowOkNs[task] = System.nanoTime() }
                    PID_ETHANOL -> readPid(task)?.let { ethanol = it[0] * 100f / 255f; slowOkNs[task] = System.nanoTime() }
                    PID_HYBRID_BATT -> {
                        val v = if (usable(PID_HYBRID_BATT)) {
                            readPid(task)?.let { it[0] * 100f / 255f }
                        } else if (prof.socQuery != null) {
                            prof.socQuery?.let { readCustomSoc(it) }
                        } else if (prof.isHybrid) {
                            readToyotaHvSoc()
                        } else null
                        if (v != null) soc = v
                    }
                    PID_MAP -> readPid(task)?.let { turbo = (it[0] - baroKpa).coerceAtLeast(0f) }
                }
            }

            // ---------- Sensores adicionais (1 por ciclo, em rotação) ----------
            if (linkOk && supported.isNotEmpty()) {
                val eligible = SensorRegistry.standard.filter { usable(it.pid) || (it.pid in probeConfirmed && it.pid !in disabledPids) }
                val wanted = priorityPids
                val priority = if (wanted.isEmpty()) emptyList() else eligible.filter { it.pid in wanted }
                if (eligible.isNotEmpty()) {
                    // Sensores que estão em manômetros ocupam ciclos alternados; os demais seguem em rotação
                    val def = if (priority.isNotEmpty() && cycle % 2 == 0) {
                        priority[priorityIdx++ % priority.size]
                    } else {
                        eligible[extraIdx++ % eligible.size]
                    }
                    readPid(def.pid)?.let { bytes ->
                        def.decode(bytes)?.let { v ->
                            extraValues[def.id] = v
                            extraOkNs[def.id] = System.nanoTime()
                        }
                    }
                }
            }

            // ---------- Sondagem de PIDs não anunciados pela ECU (1 a cada 3 ciclos) ----------
            // Só atua em sensores que hoje ficam "sem leitura" (PID fora da lista de suportados).
            // Sensores já declarados pela ECU seguem exatamente a rotina acima, sem alteração.
            if (linkOk && supported.isNotEmpty() && cycle % PROBE_EVERY_N_CYCLES == 1) {
                val candidates = (SensorRegistry.standard.map { it.pid } + PID_OIL_TEMP).distinct().filter { pid ->
                    !usable(pid) && pid !in disabledPids && pid !in probeConfirmed &&
                        (probeFails[pid] ?: 0) < PROBE_FAILS_TO_STOP
                }
                if (candidates.isNotEmpty()) {
                    val pid = candidates[probeIdx++ % candidates.size]
                    if (pid == PID_OIL_TEMP) {
                        probeUndeclaredPid(pid) { b -> if (b.isEmpty()) null else b[0] - 40f }?.let { v ->
                            oil = v
                            slowOkNs[PID_OIL_TEMP] = System.nanoTime()
                        }
                    } else {
                        SensorRegistry.standard.firstOrNull { it.pid == pid }?.let { def ->
                            probeUndeclaredPid(pid, def.decode)?.let { v ->
                                extraValues[def.id] = v
                                extraOkNs[def.id] = System.nanoTime()
                            }
                        }
                    }
                }
            }

            // ---------- Médias e tempo ----------
            val nowNs = System.nanoTime()
            val dtSec = ((nowNs - lastSampleNs) / 1_000_000_000.0).coerceIn(0.0, 2.0)
            lastSampleNs = nowNs

            if (speed > 0) {
                speedTotal += speed
                speedCount++
            }
            val avgSpeed = if (speedCount > 0) speedTotal / speedCount else 0f

            // ---------- Modo de condução / fluxo híbrido ----------
            val driveMode = when {
                prof.isHybrid && rpm < 400 && speed < 45 -> DriveMode.ECO
                load > 70f || rpm > 3200f || tps > 65f -> DriveMode.SPORT
                load < 35f && speed < 65f -> DriveMode.ECO
                else -> DriveMode.NORMAL
            }
            val hybridFlow = when {
                prof.isHybrid && tps < 3f && speed > 5f -> HybridEnergyFlowState.REGEN_BRAKING
                prof.isHybrid && rpm < 400 && speed > 0 -> HybridEnergyFlowState.EV_DRIVE
                rpm > 400 && speed > 0 -> HybridEnergyFlowState.HYBRID_DRIVE
                speed == 0f && rpm > 400 -> HybridEnergyFlowState.ENGINE_CHARGING
                else -> HybridEnergyFlowState.IDLE
            }

            // ---------- Consumo ----------
            val inmetroKmL = when (driveMode) {
                DriveMode.ECO -> prof.inmetroUrbanKmPerL.toFloat()
                DriveMode.NORMAL -> prof.inmetroAvgKmPerL.toFloat()
                DriveMode.SPORT -> (prof.inmetroRoadKmPerL * 0.8).toFloat()
            }
            // Sem vazão real, estima pela tabela INMETRO (e a UI avisa com "~")
            val estimated = !hasFlowSource
            val flowLph: Float? = when {
                !estimated -> realFlowLph
                speed > 0f -> speed / inmetroKmL
                else -> 0f
            }
            if (flowLph != null) {
                distanceKm += speed * dtSec / 3600.0
                fuelUsedL += flowLph * dtSec / 3600.0
            }
            val instantRaw = when {
                speed < 3f -> 0f
                flowLph == null -> instantEma
                flowLph > 0.1f -> (speed / flowLph).coerceAtMost(99.9f)
                else -> 99.9f // motor desligado em movimento (EV / corte de injeção)
            }
            instantEma = if (instantRaw == 0f) 0f else instantEma + (instantRaw - instantEma) * 0.35f
            val avgKmL = if (distanceKm >= 0.3 && fuelUsedL > 0.01) (distanceKm / fuelUsedL).toFloat() else 0f

            // ---------- Autonomia & Combustível ----------
            val effectiveTankCapacity = if (prof.tankCapacityLiters > 0.0) prof.tankCapacityLiters else 50.0
            val tankLitres = effectiveTankCapacity * (fuel / 100.0)
            val rangeKmL = if (avgKmL > 0f && distanceKm >= 2.0) avgKmL else inmetroKmL
            val autonomy = (tankLitres * rangeKmL).toInt()

            fun fresh(key: Int): Boolean = slowOkNs[key]?.let { nowNs - it < slowStaleNs } ?: false
            val fuelAvailable = linkOk && fresh(PID_FUEL_LEVEL)
            val usingMeasuredAvg = avgKmL > 0f && distanceKm >= 2.0

            // Só entram sensores com leitura recente; os demais ficam fora do mapa (UI mostra "--")
            val sensorSnapshot = HashMap<String, Float>()
            if (linkOk) {
                for ((id, v) in extraValues) {
                    val okNs = extraOkNs[id]
                    if (okNs != null && nowNs - okNs < extraStaleNs) sensorSnapshot[id] = v
                }
                // AFR é CALCULADO: lambda real x AFR estequiométrico da mistura (gasolina/etanol)
                val lambda = sensorSnapshot["o2_wr_b1s1_lambda"] ?: sensorSnapshot["cmd_equiv_ratio"]
                if (lambda != null) {
                    sensorSnapshot[SensorRegistry.AFR_ID] = lambda * (14.7f * (1f - ethFrac) + 9.0f * ethFrac)
                }
            }

            t = t.copy(
                speedKmh = speed,
                avgSpeedKmh = avgSpeed,
                rpm = rpm,
                coolantTempC = if (linkOk && fresh(PID_COOLANT)) coolant else Float.NaN,
                oilTempC = if (linkOk && fresh(PID_OIL_TEMP)) oil else Float.NaN,
                fuelLevelPercent = fuel,
                engineLoadPercent = if (linkOk && fresh(PID_ENGINE_LOAD)) load else 0f,
                throttlePercent = tps,
                batteryVoltage = if (fresh(-1)) batt else 0f,
                turboPressureKpa = turbo,
                ethanolPercent = if (linkOk && fresh(PID_ETHANOL)) ethanol else Float.NaN,
                driveMode = driveMode,
                hybridFlowState = hybridFlow,
                instantConsumptionKmPerL = instantEma,
                avgConsumptionKmPerL = avgKmL,
                consumptionIsEstimated = estimated,
                autonomyKm = autonomy,
                elapsedTimeSeconds = (nowNs - startNs) / 1_000_000_000L,
                isEngineRunning = rpm > 300,
                isEvMode = prof.isHybrid && rpm < 300,
                hybridBatterySocPercent = if (soc.isNaN()) 0f else soc,
                hybridSocAvailable = !soc.isNaN() && linkOk,
                hybridDetailAvailable = false,
                ecuLinkOk = linkOk,
                fuelLevelAvailable = fuelAvailable,
                fuelLiters = if (fuelAvailable) (effectiveTankCapacity * (fuel / 100.0)).toFloat() else Float.NaN,
                autonomyIsEstimated = !usingMeasuredAvg,
                sensors = sensorSnapshot
            )

            withContext(Dispatchers.Main) { onTelemetryUpdate(t) }

            // Resumo periódico no terminal de diagnóstico (a cada ~10 s)
            cycle++
            cyclesSinceSummary++
            if (nowNs - lastSummaryNs > 10_000_000_000L) {
                val hz = cyclesSinceSummary / ((nowNs - lastSummaryNs) / 1e9)
                addLog("Leitura OK: %.1f ciclos/s | vel %.0f km/h | rpm %.0f".format(hz, speed, rpm))
                lastSummaryNs = nowNs
                cyclesSinceSummary = 0
            }
            delay(20)
        }
    }
}
