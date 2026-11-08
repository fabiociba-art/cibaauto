package com.example.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.reflect.Method
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gerenciador REAL de comunicação OBD-II via ELM327.
 * Sem nenhuma simulação.
 *
 * Suporta:
 *  - Bluetooth Classic SPP (UUID padrão ELM327)
 *  - Fallback por reflexão (createRfcommSocket) para clones baratos
 *  - Wi-Fi / TCP (IP padrão 192.168.0.10:35000)
 */
class ObdManager {

    companion object {
        private const val TAG = "CibaObdManager"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val WIFI_DEFAULT_HOST = "192.168.0.10"
        private const val WIFI_DEFAULT_PORT = 35000
        private const val CMD_TIMEOUT_MS = 3000L

        /**
         * O PID Mode 21 "CE" original NÃO é verificado para o Corolla 2026 (os PIDs Toyota
         * mudam por geração: os do Prius Gen3 não servem no Corolla híbrido). Enquanto não
         * houver um PID confirmado via [probeHybrid], deixe false para não exibir lixo na tela.
         */
        private const val EXPERIMENTAL_TOYOTA_MODE21 = false
    }

    private val initializationCommands = listOf(
        "ATZ",   // Reset
        "ATE0",  // Echo off
        "ATL0",  // Linefeeds off
        "ATS0",  // Spaces off
        "ATH0",  // Headers off
        "ATSP0", // Auto protocol
        "ATCAF1", // CAN auto-formatting (ELM monta os bytes PCI do ISO-TP)
        "ATAT1", // Adaptive timing
        "ATST64" // Timeout de resposta ~400 ms (ECUs híbridas Toyota são mais lentas)
    )

    private var bluetoothSocket: BluetoothSocket? = null
    private var wifiSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputReader: BufferedReader? = null
    private val isConnected = AtomicBoolean(false)
    private var connectionType: ConnectionType = ConnectionType.NONE

    enum class ConnectionType { NONE, BLUETOOTH, WIFI }

    data class PidResult(
        val rpm: Int? = null,
        val speed: Float? = null,
        val coolantTemp: Int? = null,
        val intakeTemp: Int? = null,
        val engineLoad: Int? = null,
        val manifoldPressure: Float? = null,
        val throttlePosition: Int? = null,
        val batteryVoltage: Float? = null,
        val ethanolPercentage: Int? = null,
        val maf: Float? = null,
        val fuelPressure: Float? = null,
        val odometerKm: Float? = null,
        val fuelLevelPercent: Float? = null,
        val hybridBatterySoc: Int? = null,
        val hybridMotorPowerKw: Float? = null,
        val isHybridDetected: Boolean = false,
        val oilTemp: Int? = null,
        val ambientTemp: Int? = null,
        val catalystTemp: Int? = null,
        val hybridExtras: Map<String, String> = emptyMap()
    )

    /** Dados do Mode 09 (VIN, calibração) + tipo de combustível (01 51). */
    data class VehicleInfo(
        val vin: String = "",
        val calibrationId: String = "",
        val fuelType: String = ""
    )

    /**
     * PID proprietário do híbrido (Mode 21/22) a ser lido continuamente.
     * A lista [hybridPidDefs] está VAZIA de propósito: só preencher com PIDs confirmados
     * pela varredura (probeHybrid) no Corolla 2026 — chutar PID mostra número errado na tela.
     * responseTag = bytes de eco da resposta (ex.: "61CE"); byteOffset conta a partir do 1º byte de dados.
     */
    data class HybridPidDef(
        val key: String,
        val label: String,
        val unit: String,
        val header: String,
        val command: String,
        val responseTag: String,
        val byteOffset: Int,
        val byteCount: Int = 1,
        val scale: Float = 1f,
        val offset: Float = 0f,
        val signed: Boolean = false
    )

    private val hybridPidDefs: List<HybridPidDef> = emptyList()

    data class DtcResult(
        val codes: List<String>,
        val milOn: Boolean
    )

    /** Info de dispositivo Bluetooth para a UI */
    data class BtDeviceInfo(
        val name: String,
        val address: String,
        val isLikelyObd: Boolean
    )

    /**
     * Lista dispositivos Bluetooth já pareados no sistema.
     * Marca como "provável OBD" se o nome contiver palavras-chave conhecidas.
     */
    @SuppressLint("MissingPermission")
    fun listPairedDevices(
        preferredNameHints: List<String> = listOf(
            "OBD", "ELM", "Vgate", "OBDII", "OBD-II", "Android-Vlink",
            "BT", "VLink", "OBDLink", "ScanTool", "Carista", "KONNWEI",
            "Viecar", "Bafx", "FIXD", "BlueDriver", "ADB"
        )
    ): List<BtDeviceInfo> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        val bonded = adapter.bondedDevices ?: return emptyList()
        return bonded.map { device ->
            val name = device.name ?: "Dispositivo sem nome"
            val upper = name.uppercase()
            val likely = preferredNameHints.any { hint -> upper.contains(hint.uppercase()) }
            BtDeviceInfo(name = name, address = device.address, isLikelyObd = likely)
        }.sortedWith(compareByDescending<BtDeviceInfo> { it.isLikelyObd }.thenBy { it.name })
    }

    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }

    /**
     * Conecta em um dispositivo específico pelo endereço MAC.
     */
    @SuppressLint("MissingPermission")
    suspend fun connectBluetoothToAddress(
        address: String,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnectInternal()
            onLog("Conectando ao dispositivo selecionado: $address")

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                onLog("Bluetooth desligado ou indisponível.")
                return@withContext false
            }

            val device = try {
                adapter.getRemoteDevice(address)
            } catch (e: Exception) {
                onLog("Endereço Bluetooth inválido: $address")
                return@withContext false
            }

            val name = device.name ?: address
            onLog("Dispositivo: $name ($address)")

            val socket = tryCreateSocket(device, onLog)
            if (socket == null) {
                onLog("Não foi possível criar socket RFCOMM para este dispositivo.")
                return@withContext false
            }

            try {
                adapter.cancelDiscovery()
                if (!socket.isConnected) {
                    socket.connect()
                }
                bluetoothSocket = socket
                outputStream = socket.outputStream
                inputReader = BufferedReader(InputStreamReader(socket.inputStream))
                connectionType = ConnectionType.BLUETOOTH
                isConnected.set(true)

                onLog("Socket aberto. Inicializando ELM327...")
                val ok = runInitialization(onLog)
                if (ok) {
                    onLog("Conexão Bluetooth OBD-2 estabelecida com $name.")
                    true
                } else {
                    onLog("Inicialização do ELM327 falhou neste dispositivo.")
                    disconnectInternal()
                    false
                }
            } catch (e: Exception) {
                onLog("Falha ao conectar em $name: ${e.message}")
                try { socket.close() } catch (_: Exception) {}
                disconnectInternal()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "connectBluetoothToAddress error", e)
            onLog("Erro: ${e.message}")
            disconnectInternal()
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectBluetooth(
        preferredNameHints: List<String> = listOf(
            "OBD", "ELM", "Vgate", "OBDII", "OBD-II", "Android-Vlink",
            "BT", "VLink", "OBDLink", "ScanTool", "Carista", "KONNWEI",
            "Viecar", "Bafx", "FIXD", "BlueDriver"
        ),
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnectInternal()
            onLog("Procurando adaptador Bluetooth ELM327 pareado...")

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                onLog("Bluetooth não disponível neste aparelho.")
                return@withContext false
            }
            if (!adapter.isEnabled) {
                onLog("Bluetooth está desligado. Ligue o Bluetooth e tente novamente.")
                return@withContext false
            }

            val bonded = adapter.bondedDevices ?: emptySet()
            onLog("Dispositivos Bluetooth pareados: ${bonded.size}")

            if (bonded.isEmpty()) {
                onLog("Nenhum dispositivo pareado. Vá em Configurações > Bluetooth e pareie o ELM327 primeiro.")
                return@withContext false
            }

            // Lista todos os pareados no log para debug
            bonded.forEach { d ->
                onLog("  → ${d.name ?: "sem nome"} [${d.address}]")
            }

            // Prioriza dispositivos cujo nome contenha hints conhecidos de OBD
            val candidates = bonded.filter { device ->
                val name = device.name?.uppercase() ?: ""
                preferredNameHints.any { hint -> name.contains(hint.uppercase()) }
            }.ifEmpty { bonded.toList() }

            var lastError: String? = null

            for (candidate in candidates) {
                onLog("Tentando: ${candidate.name} (${candidate.address})")
                val socket = tryCreateSocket(candidate, onLog)
                if (socket != null) {
                    try {
                        adapter.cancelDiscovery()
                        if (!socket.isConnected) {
                            socket.connect()
                        }
                        bluetoothSocket = socket
                        outputStream = socket.outputStream
                        inputReader = BufferedReader(InputStreamReader(socket.inputStream))
                        connectionType = ConnectionType.BLUETOOTH
                        isConnected.set(true)

                        onLog("Socket Bluetooth aberto. Inicializando ELM327...")
                        val ok = runInitialization(onLog)
                        if (ok) {
                            onLog("Conexão Bluetooth OBD-2 estabelecida com ${candidate.name}.")
                            return@withContext true
                        } else {
                            onLog("Inicialização falhou neste dispositivo. Tentando próximo...")
                            disconnectInternal()
                        }
                    } catch (e: Exception) {
                        lastError = e.message
                        onLog("Falha ao conectar em ${candidate.name}: ${e.message}")
                        try { socket.close() } catch (_: Exception) {}
                        disconnectInternal()
                    }
                }
            }

            onLog("Não foi possível conectar a nenhum dispositivo pareado. Último erro: $lastError")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth connect error", e)
            onLog("Erro Bluetooth: ${e.message}")
            disconnectInternal()
            false
        }
    }

    /**
     * Tenta criar socket de duas formas:
     * 1. createRfcommSocketToServiceRecord (padrão)
     * 2. Reflexão createRfcommSocket(int channel) – necessário em muitos clones chineses
     */
    @SuppressLint("MissingPermission")
    private fun tryCreateSocket(device: BluetoothDevice, onLog: (String) -> Unit): BluetoothSocket? {
        // Método 1: UUID padrão SPP
        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            onLog("Socket criado via UUID SPP.")
            return socket
        } catch (e: Exception) {
            onLog("createRfcommSocketToServiceRecord falhou: ${e.message}")
        }

        // Método 2: reflexão (channel 1) – muito comum em ELM327 genéricos
        try {
            val method: Method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val socket = method.invoke(device, 1) as BluetoothSocket
            onLog("Socket criado via reflexão (channel 1).")
            return socket
        } catch (e: Exception) {
            onLog("Reflexão createRfcommSocket falhou: ${e.message}")
        }

        // Método 3: insecure
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            onLog("Socket criado via InsecureRfcomm.")
            return socket
        } catch (e: Exception) {
            onLog("createInsecureRfcommSocketToServiceRecord falhou: ${e.message}")
        }

        return null
    }

    suspend fun connectWifi(
        host: String = WIFI_DEFAULT_HOST,
        port: Int = WIFI_DEFAULT_PORT,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnectInternal()
            onLog("Tentando conexão Wi-Fi ELM327 em $host:$port ...")

            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.soTimeout = CMD_TIMEOUT_MS.toInt()

            wifiSocket = socket
            outputStream = socket.getOutputStream()
            inputReader = BufferedReader(InputStreamReader(socket.getInputStream()))
            connectionType = ConnectionType.WIFI
            isConnected.set(true)

            onLog("Socket TCP aberto. Inicializando ELM327...")
            val ok = runInitialization(onLog)
            if (!ok) {
                disconnectInternal()
                return@withContext false
            }
            onLog("Conexão Wi-Fi OBD-2 estabelecida ($host).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "WiFi connect error", e)
            onLog("Erro Wi-Fi ($host): ${e.message}")
            disconnectInternal()
            false
        }
    }

    /**
     * Tenta Bluetooth primeiro; se falhar, tenta Wi-Fi no IP clássico.
     */
    suspend fun connectAuto(onLog: (String) -> Unit): Boolean {
        val btOk = connectBluetooth(onLog = onLog)
        if (btOk) return true
        onLog("Bluetooth falhou. Tentando Wi-Fi (192.168.0.10:35000)...")
        return connectWifi(onLog = onLog)
    }

    private suspend fun runInitialization(onLog: (String) -> Unit): Boolean {
        // Aguarda o ELM327 resetar após ATZ
        delay(1200)

        for (cmd in initializationCommands) {
            onLog(">> $cmd")
            val resp = sendCommand(cmd)
            onLog("<< ${resp.trim()}")

            // ATZ sempre retorna algo com "ELM" ou versão; se der timeout total, falha
            if (cmd == "ATZ") {
                if (resp.isBlank() || resp.contains("ERROR", ignoreCase = true) || resp.contains("NO STREAM") || resp.contains("NOT CONNECTED")) {
                    onLog("Falha crítica no ATZ – adaptador não respondeu.")
                    return false
                }
            } else if (resp.contains("ERROR", ignoreCase = true) || resp.contains("?")) {
                // Alguns clones respondem "?" em comandos opcionais; não é fatal
                if (cmd in listOf("ATAT1", "ATST64", "ATCAF1")) {
                    onLog("Comando opcional $cmd ignorado.")
                } else {
                    onLog("Aviso no comando $cmd: $resp")
                }
            }
            delay(150)
        }
        detectProtocol(onLog)
        return true
    }

    /** Protocolo detectado (resultado do ATDPN), ex.: "A6" = auto + ISO 15765-4 CAN 11bit 500k. */
    var detectedProtocol: String = ""
        private set

    /** true quando a ECU respondeu ao Mode 01 PID 00 (ignição ligada e protocolo resolvido). */
    var ecuResponding: Boolean = false
        private set

    /**
     * ATSP0 faz o ELM327 procurar o protocolo na PRIMEIRA consulta ("SEARCHING...").
     * Isso leva vários segundos e estourava o timeout de 3 s: a resposta era descartada no
     * próximo comando e o app ficava "conectado" mas sem nenhum dado.
     * Aqui forçamos essa primeira consulta com timeout longo e, se falhar, tentamos
     * fixar CAN 11-bit/500k (protocolo 6, usado pelo Corolla 2020+).
     */
    private suspend fun detectProtocol(onLog: (String) -> Unit) {
        ecuResponding = false
        onLog("Detectando protocolo da ECU (pode levar até 15 s). Ignição ligada / carro em READY?")
        var resp = sendCommand("0100", 15000L)
        onLog("<< 0100: ${resp.trim()}")
        if (!resp.contains("41", ignoreCase = true) || resp.contains("UNABLE", true) || resp.contains("ERROR", true)) {
            onLog("Auto-detecção falhou. Fixando CAN 11-bit 500k (ATSP6)...")
            sendCommand("ATSP6")
            resp = sendCommand("0100", 8000L)
            onLog("<< 0100: ${resp.trim()}")
        }
        ecuResponding = cleanHex(resp).contains("4100")
        detectedProtocol = sendCommand("ATDPN").trim()
        onLog("Protocolo: $detectedProtocol (${sendCommand("ATDP").trim()})")
        onLog(
            if (ecuResponding) "ECU respondeu ao Mode 01. Leitura habilitada."
            else "ECU NÃO respondeu. Ligue o carro (READY), confira o adaptador na porta OBD e tente de novo."
        )
    }

    suspend fun sendCommand(command: String, timeoutMs: Long = CMD_TIMEOUT_MS): String = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext "NOT CONNECTED"
        try {
            val out = outputStream ?: return@withContext "NO STREAM"
            val reader = inputReader ?: return@withContext "NO READER"

            // Limpa qualquer dado residual no buffer
            while (reader.ready()) {
                reader.read()
            }

            out.write("$command\r".toByteArray(Charsets.US_ASCII))
            out.flush()

            val response = StringBuilder()
            val start = System.currentTimeMillis()
            var foundPrompt = false

            while (System.currentTimeMillis() - start < timeoutMs && !foundPrompt) {
                if (reader.ready()) {
                    val ch = reader.read()
                    if (ch == -1) break
                    val c = ch.toChar()
                    if (c == '>') {
                        foundPrompt = true
                    } else {
                        response.append(c)
                    }
                } else {
                    delay(15)
                }
            }

            // Remove echo do comando e limpa
            var cleaned = response.toString()
                .replace(command, "", ignoreCase = true)
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            cleaned
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand error", e)
            "ERROR: ${e.message}"
        }
    }

    private var pidCycleCount = 0

    /**
     * Lê os PIDs principais (Mode 01) e opcionalmente dados do sistema Híbrido Toyota (Mode 21 via Header 7E2).
     */
    suspend fun readMainPids(queryHybrid: Boolean = false): PidResult = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext PidResult()
        pidCycleCount++

        val rpm = parseRpm(sendCommand("010C"))
        val speed = parseSpeed(sendCommand("010D"))
        val coolant = parseTemp(sendCommand("0105"))
        val intake = parseTemp(sendCommand("010F"))
        val load = parsePercent(sendCommand("0104"))
        val map = parseMap(sendCommand("010B"))
        val tps = parsePercent(sendCommand("0111"))
        val voltage = parseVoltage(sendCommand("0142"))
        val maf = parseMaf(sendCommand("0110"))
        val fuelPress = parseFuelPressure(sendCommand("010A"))

        // PIDs de menor frequência (economiza banda no barramento e acelera medidores rápidos)
        val ethanol = if (pidCycleCount % 6 == 0 || pidCycleCount == 1) parseEthanol(sendCommand("0152")) else null
        val fuelLevel = if (pidCycleCount % 10 == 0 || pidCycleCount == 1) parseFuelLevel(sendCommand("012F")) else null
        val odo = if (pidCycleCount % 15 == 0 || pidCycleCount == 1) parseOdometer(sendCommand("01A6")) else null

        // Sistema híbrido: 1) PID padrão SAE 01 5B (vida restante da bateria HV) e
        // 2) Mode 21 proprietário (experimental, desligado por padrão — veja EXPERIMENTAL_TOYOTA_MODE21).
        var hybridSoc: Int? = null
        var hybridKw: Float? = null
        var hybridDetected = false

        if (queryHybrid && (pidCycleCount % 4 == 0 || pidCycleCount <= 2)) {
            val std = parseHybridPackLife(sendCommand("015B"))
            if (std != null) {
                hybridSoc = std
                hybridDetected = true
            }
            if (EXPERIMENTAL_TOYOTA_MODE21) {
                val hybridData = readToyotaHybridData()
                if (hybridData != null) {
                    hybridSoc = hybridData.first
                    hybridKw = hybridData.second
                    hybridDetected = true
                }
            }
        }

        // Temperaturas extras (nem todo carro responde; null = não suportado)
        val extraTemps = pidCycleCount % 6 == 2 || pidCycleCount == 1
        val oilT = if (extraTemps) parseSimpleTemp(sendCommand("015C"), "415C") else null
        val ambT = if (extraTemps) parseSimpleTemp(sendCommand("0146"), "4146") else null
        val catT = if (extraTemps) parseCatalystTemp(sendCommand("013C")) else null

        val extras = if (queryHybrid && hybridPidDefs.isNotEmpty() && pidCycleCount % 3 == 0) readHybridExtras() else emptyMap()

        PidResult(
            rpm = rpm,
            speed = speed,
            coolantTemp = coolant,
            intakeTemp = intake,
            engineLoad = load,
            manifoldPressure = map,
            throttlePosition = tps,
            batteryVoltage = voltage,
            ethanolPercentage = ethanol,
            maf = maf,
            fuelPressure = fuelPress,
            odometerKm = odo,
            fuelLevelPercent = fuelLevel,
            hybridBatterySoc = hybridSoc,
            hybridMotorPowerKw = hybridKw,
            isHybridDetected = hybridDetected,
            oilTemp = oilT,
            ambientTemp = ambT,
            catalystTemp = catT,
            hybridExtras = extras
        )
    }

    private suspend fun readHybridExtras(): Map<String, String> {
        val out = mutableMapOf<String, String>()
        try {
            for ((header, defs) in hybridPidDefs.groupBy { it.header }) {
                sendCommand("ATSH$header")
                for (d in defs) {
                    val c = cleanHex(sendCommand(d.command))
                    val idx = c.indexOf(d.responseTag.uppercase())
                    if (idx < 0) continue
                    val start = idx + d.responseTag.length + d.byteOffset * 2
                    val end = start + d.byteCount * 2
                    if (c.length < end) continue
                    var raw = c.substring(start, end).toLong(16)
                    if (d.signed) {
                        val bits = d.byteCount * 8
                        if (raw >= (1L shl (bits - 1))) raw -= (1L shl bits)
                    }
                    out[d.label + "|" + d.unit] = String.format("%.1f", raw * d.scale + d.offset)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Hybrid extras error: ${e.message}")
        } finally {
            try { sendCommand("ATSH7DF") } catch (_: Exception) {}
        }
        return out
    }

    /** Mode 09 02 (VIN), 09 04 (Calibration ID) e 01 51 (tipo de combustível). */
    suspend fun readVehicleInfo(): VehicleInfo = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext VehicleInfo()
        val vin = parseMode09Ascii(sendCommand("0902", 6000L), "490201").filter { it.isLetterOrDigit() }.takeLast(17)
        val cal = parseMode09Ascii(sendCommand("0904", 6000L), "490401").trim()
        val fuel = parseFuelType(sendCommand("0151"))
        VehicleInfo(vin = vin, calibrationId = cal, fuelType = fuel)
    }

    /**
     * Respostas multi-frame chegam como "014 0: 49 02 01 .. 1: .. 2: ..".
     * Remove o tamanho e os índices "N:", junta os bytes e converte para ASCII.
     */
    private fun parseMode09Ascii(resp: String, tag: String): String {
        val tokens = resp.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val sb = StringBuilder()
        for ((i, t0) in tokens.withIndex()) {
            var t = t0
            if (i == 0 && t.length == 3 && !t.contains(":") && t.all { it.isLetterOrDigit() } && tokens.size > 1 && tokens[1].contains(":")) continue
            val colon = t.indexOf(':')
            if (colon >= 0) t = t.substring(colon + 1)
            sb.append(t)
        }
        val hex = sb.toString().replace(" ", "").uppercase()
        val idx = hex.indexOf(tag)
        if (idx < 0) return ""
        val data = hex.substring(idx + tag.length)
        val chars = StringBuilder()
        var i = 0
        while (i + 2 <= data.length) {
            val b = data.substring(i, i + 2).toIntOrNull(16) ?: break
            if (b in 32..126) chars.append(b.toChar())
            i += 2
        }
        return chars.toString()
    }

    private fun parseFuelType(resp: String): String {
        val c = cleanHex(resp)
        val idx = c.indexOf("4151")
        if (idx < 0 || c.length < idx + 6) return ""
        return when (c.substring(idx + 4, idx + 6).toIntOrNull(16)) {
            0x01 -> "Gasolina"
            0x02 -> "Metanol"
            0x03 -> "Etanol"
            0x04 -> "Diesel"
            0x05 -> "GLP"
            0x06 -> "GNV"
            0x07 -> "Propano"
            0x08 -> "Elétrico"
            0x09 -> "Bi-combustível (gasolina)"
            0x0A -> "Bi-combustível (metanol)"
            0x0B -> "Bi-combustível (etanol)"
            0x0C -> "Bi-combustível (GLP)"
            0x0D -> "Bi-combustível (GNV)"
            0x0E -> "Bi-combustível (propano)"
            0x0F -> "Bi-combustível (elétrico)"
            0x10 -> "Bi-combustível (elétrico/combustão)"
            0x11 -> "Híbrido (gasolina)"
            0x12 -> "Híbrido (etanol)"
            0x13 -> "Híbrido (diesel)"
            0x14 -> "Híbrido (elétrico)"
            0x15 -> "Híbrido (misto)"
            0x16 -> "Híbrido regenerativo"
            else -> ""
        }
    }

    private fun parseSimpleTemp(resp: String, tag: String): Int? {
        val c = cleanHex(resp)
        val idx = c.indexOf(tag)
        if (idx < 0 || c.length < idx + 6) return null
        return try { c.substring(idx + 4, idx + 6).toInt(16) - 40 } catch (_: Exception) { null }
    }

    private fun parseCatalystTemp(resp: String): Int? {
        val c = cleanHex(resp)
        val idx = c.indexOf("413C")
        if (idx < 0 || c.length < idx + 8) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toInt(16)
            val b = c.substring(idx + 6, idx + 8).toInt(16)
            (((a * 256) + b) / 10f - 40f).toInt()
        } catch (_: Exception) { null }
    }

    /**
     * Mode 03 – códigos de falha armazenados.
     */
    suspend fun readDtcs(): DtcResult = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext DtcResult(emptyList(), false)

        val status = sendCommand("0101")
        val milOn = status.contains("41 01") || status.contains("4101") && run {
            try {
                val clean = cleanHex(status)
                val idx = clean.indexOf("4101")
                if (idx >= 0 && clean.length >= idx + 6) {
                    val byteA = clean.substring(idx + 4, idx + 6).toInt(16)
                    (byteA and 0x80) != 0
                } else false
            } catch (_: Exception) { false }
        }

        val raw = sendCommand("03")
        val codes = parseDtcResponse(raw)
        DtcResult(codes, milOn)
    }

    /**
     * Mode 04 – limpar códigos e resetar MIL.
     */
    suspend fun clearDtcs(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext false
        val resp = sendCommand("04")
        resp.contains("44") || resp.contains("OK", ignoreCase = true)
    }

    /**
     * Mode 01 PID 01 – monitores de prontidão (simplificado).
     */
    suspend fun readReadiness(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext emptyMap()
        val resp = sendCommand("0101")
        val map = mutableMapOf<String, String>()
        map["MIL"] = if (resp.contains("41 01") || resp.contains("4101")) "Lido" else "Indisponível"
        map["Raw"] = resp.take(60)
        map
    }

    fun isConnected(): Boolean = isConnected.get()

    fun getConnectionType(): ConnectionType = connectionType

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        try { inputReader?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { bluetoothSocket?.close() } catch (_: Exception) {}
        try { wifiSocket?.close() } catch (_: Exception) {}
        inputReader = null
        outputStream = null
        bluetoothSocket = null
        wifiSocket = null
        isConnected.set(false)
        connectionType = ConnectionType.NONE
    }

    // -------------------- Parsers Mode 01 --------------------

    private fun cleanHex(response: String): String {
        return response
            .replace("SEARCHING...", "", ignoreCase = true)
            .replace("STOPPED", "", ignoreCase = true)
            .replace("UNABLE TO CONNECT", "", ignoreCase = true)
            .replace("NO DATA", "", ignoreCase = true)
            .replace(">", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
            .uppercase()
            .trim()
    }

    private fun parseRpm(resp: String): Int? {
        val c = cleanHex(resp)
        val idx = c.indexOf("410C")
        if (idx < 0 || c.length < idx + 8) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toInt(16)
            val b = c.substring(idx + 6, idx + 8).toInt(16)
            ((a * 256) + b) / 4
        } catch (_: Exception) { null }
    }

    private fun parseSpeed(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("410D")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            c.substring(idx + 4, idx + 6).toInt(16).toFloat()
        } catch (_: Exception) { null }
    }

    private fun parseTemp(resp: String): Int? {
        val c = cleanHex(resp)
        val idx5 = c.indexOf("4105")
        val idxF = c.indexOf("410F")
        val idx = when {
            idx5 >= 0 -> idx5
            idxF >= 0 -> idxF
            else -> return null
        }
        if (c.length < idx + 6) return null
        return try {
            c.substring(idx + 4, idx + 6).toInt(16) - 40
        } catch (_: Exception) { null }
    }

    private fun parsePercent(resp: String): Int? {
        val c = cleanHex(resp)
        val idx4 = c.indexOf("4104")
        val idx11 = c.indexOf("4111")
        val idx = when {
            idx4 >= 0 -> idx4
            idx11 >= 0 -> idx11
            else -> return null
        }
        if (c.length < idx + 6) return null
        return try {
            (c.substring(idx + 4, idx + 6).toInt(16) * 100) / 255
        } catch (_: Exception) { null }
    }

    private fun parseMap(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("410B")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            c.substring(idx + 4, idx + 6).toInt(16).toFloat()
        } catch (_: Exception) { null }
    }

    private fun parseVoltage(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("4142")
        if (idx < 0 || c.length < idx + 8) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toInt(16)
            val b = c.substring(idx + 6, idx + 8).toInt(16)
            ((a * 256) + b) / 1000f
        } catch (_: Exception) { null }
    }

    private fun parseEthanol(resp: String): Int? {
        val c = cleanHex(resp)
        val idx = c.indexOf("4152")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            (c.substring(idx + 4, idx + 6).toInt(16) * 100) / 255
        } catch (_: Exception) { null }
    }

    private fun parseMaf(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("4110")
        if (idx < 0 || c.length < idx + 8) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toInt(16)
            val b = c.substring(idx + 6, idx + 8).toInt(16)
            ((a * 256) + b) / 100f
        } catch (_: Exception) { null }
    }

    private fun parseFuelPressure(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("410A")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            c.substring(idx + 4, idx + 6).toInt(16) * 3f
        } catch (_: Exception) { null }
    }

    private fun parseOdometer(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("41A6")
        if (idx < 0 || c.length < idx + 12) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toLong(16)
            val b = c.substring(idx + 6, idx + 8).toLong(16)
            val cByte = c.substring(idx + 8, idx + 10).toLong(16)
            val d = c.substring(idx + 10, idx + 12).toLong(16)
            val raw = (a shl 24) or (b shl 16) or (cByte shl 8) or d
            val km = raw / 10f
            if (km in 0f..3_000_000f) km else null
        } catch (_: Exception) { null }
    }

    private fun parseFuelLevel(resp: String): Float? {
        val c = cleanHex(resp)
        val idx = c.indexOf("412F")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            val a = c.substring(idx + 4, idx + 6).toInt(16)
            (a * 100f) / 255f
        } catch (_: Exception) { null }
    }

    /**
     * Tenta ler telemetria do sistema híbrido Toyota via CAN Header 7E2 (HV Control ECU)
     * e comando Mode 21 PID CE (Toyota Enhanced Diagnostic).
     * Restaura sempre o cabeçalho 7DF (Broadcast) ao finalizar.
     */
    private suspend fun readToyotaHybridData(): Pair<Int, Float>? {
        try {
            // Configura header CAN para o módulo de controle híbrido Toyota (7E2)
            val shResp = sendCommand("ATSH7E2")
            if (shResp.contains("ERROR")) {
                sendCommand("ATSH7DF")
                return null
            }

            // Mode 21 PID CE: Status da bateria de tração (SOC, corrente, tensão)
            val raw = sendCommand("21CE")

            // Restaura header padrão de broadcast OBD-II (7DF)
            sendCommand("ATSH7DF")

            val clean = cleanHex(raw)
            val idx = clean.indexOf("61CE")
            if (idx >= 0 && clean.length >= idx + 8) {
                // Byte A: SOC (0-200 -> divide por 2 para 0-100%)
                val byteA = clean.substring(idx + 4, idx + 6).toInt(16)
                val soc = (byteA * 0.5f).toInt().coerceIn(0, 100)

                // Bytes subsequentes: corrente e potência do sistema híbrido
                var powerKw = 0f
                if (clean.length >= idx + 12) {
                    try {
                        val byteB = clean.substring(idx + 6, idx + 8).toInt(16)
                        val byteC = clean.substring(idx + 8, idx + 10).toInt(16)
                        val rawCurrent = (byteB * 256 + byteC) - 32768
                        val currentAmps = rawCurrent / 100f
                        // Tensão nominal aproximada do pack HV do Corolla Hybrid ~ 207V
                        powerKw = (207.2f * currentAmps / 1000f).coerceIn(-40f, 53f)
                    } catch (_: Exception) {}
                }
                return Pair(soc, powerKw)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Hybrid read error: ${e.message}")
            try { sendCommand("ATSH7DF") } catch (_: Exception) {}
        }
        return null
    }

    /** SAE J1979 Mode 01 PID 5B: vida restante do pack híbrido = A * 100 / 255 (%). */
    private fun parseHybridPackLife(resp: String): Int? {
        val c = cleanHex(resp)
        val idx = c.indexOf("415B")
        if (idx < 0 || c.length < idx + 6) return null
        return try {
            (c.substring(idx + 4, idx + 6).toInt(16) * 100) / 255
        } catch (_: Exception) { null }
    }

    /**
     * Decodifica a resposta de 01 00/20/40/60/80/A0 em lista de PIDs suportados.
     * base = 0x00, 0x20, ...
     */
    private fun decodeSupportedPids(resp: String, base: Int): List<Int> {
        val c = cleanHex(resp)
        val tag = "41" + String.format("%02X", base)
        val idx = c.indexOf(tag)
        if (idx < 0 || c.length < idx + 4 + 8) return emptyList()
        return try {
            val mask = c.substring(idx + 4, idx + 12).toLong(16)
            (1..32).filter { bit -> (mask shr (32 - bit)) and 1L == 1L }.map { base + it }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Varredura SOMENTE LEITURA para descobrir o que o híbrido responde.
     * Envia apenas Mode 01 / 21 / 22 (leitura). Usa ATH1 para mostrar QUAL ECU respondeu
     * (7E8 = motor, 7EA = ECU híbrida, 7EB = bateria HV, etc.).
     * Compartilhe o relatório para mapear os PIDs reais do Corolla 2026.
     */
    suspend fun probeHybrid(onLine: (String) -> Unit) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) { onLine("Não conectado."); return@withContext }
        onLine("== VARREDURA HÍBRIDO (somente leitura) ==")
        onLine("Protocolo: $detectedProtocol | ECU respondendo: $ecuResponding")
        sendCommand("ATH1")
        sendCommand("ATS1")
        try {
            // 1) PIDs padrão suportados
            var all = emptyList<Int>()
            for (base in listOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0)) {
                val cmd = "01" + String.format("%02X", base)
                val r = sendCommand(cmd, 6000L)
                onLine("$cmd -> $r")
                val list = decodeSupportedPids(r, base)
                all = all + list
                if (!list.contains(base + 0x20)) break
            }
            onLine("PIDs Mode 01 suportados: " + all.joinToString(" ") { String.format("%02X", it) })
            onLine("01 5B (bateria HV padrão): " + (if (all.contains(0x5B)) "SUPORTADO" else "não suportado"))
            onLine("015B -> " + sendCommand("015B"))

            // 2) Sondagem dos módulos híbridos Toyota (leitura Mode 21 / 22)
            val headers = listOf("7E2", "7E3", "7E0")
            val probes = listOf("2101", "2102", "2103", "2104", "2105", "2106", "2107", "2108", "2109", "2181", "21CE", "2201", "22F190")
            for (h in headers) {
                val sh = sendCommand("ATSH$h")
                onLine("-- header $h ($sh) --")
                onLine("0100 -> " + sendCommand("0100", 4000L))
                for (cmd in probes) {
                    val r = sendCommand(cmd, 3000L)
                    if (r.isNotBlank() && !r.contains("NO DATA", true)) onLine("$cmd -> $r")
                }
            }
        } finally {
            sendCommand("ATSH7DF")
            sendCommand("ATAR")
            sendCommand("ATH0")
            sendCommand("ATS0")
        }
        onLine("== FIM DA VARREDURA ==")
    }

    private fun parseDtcResponse(raw: String): List<String> {
        val clean = cleanHex(raw)
        // Procura 43XX...
        val idx = clean.indexOf("43")
        if (idx < 0) return emptyList()

        var payload = clean.substring(idx + 2)
        // Em CAN (ISO 15765-4) a resposta traz um byte com a quantidade de DTCs depois do 43.
        if ((payload.length / 2) % 2 == 1) payload = payload.substring(2)

        val codes = mutableListOf<String>()
        var i = 0
        while (i + 4 <= payload.length) {
            val b1 = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val b2 = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            if (b1 == 0 && b2 == 0) break

            val firstChar = when (b1 shr 6) {
                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                else -> 'U'
            }
            // Formato correto: letra + 2º dígito (bits 5-4) + nibble baixo hex + byte 2 em hex
            val code = "$firstChar${(b1 shr 4) and 0x3}${String.format("%X", b1 and 0x0F)}${String.format("%02X", b2)}"
            codes.add(code)
            i += 4
            if (codes.size >= 16) break
        }
        return codes
    }
}
