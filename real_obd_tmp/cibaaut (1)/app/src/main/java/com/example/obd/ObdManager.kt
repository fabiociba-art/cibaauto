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
    }

    private val initializationCommands = listOf(
        "ATZ",   // Reset
        "ATE0",  // Echo off
        "ATL0",  // Linefeeds off
        "ATS0",  // Spaces off
        "ATH0",  // Headers off
        "ATSP0", // Auto protocol
        "ATAT1", // Adaptive timing
        "ATST32" // Timeout
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
        val fuelPressure: Float? = null
    )

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
                if (cmd in listOf("ATAT1", "ATST32")) {
                    onLog("Comando opcional $cmd ignorado.")
                } else {
                    onLog("Aviso no comando $cmd: $resp")
                }
            }
            delay(150)
        }
        return true
    }

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
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

            while (System.currentTimeMillis() - start < CMD_TIMEOUT_MS && !foundPrompt) {
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

    /**
     * Lê os PIDs principais (Mode 01).
     */
    suspend fun readMainPids(): PidResult = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext PidResult()

        val rpm = parseRpm(sendCommand("010C"))
        val speed = parseSpeed(sendCommand("010D"))
        val coolant = parseTemp(sendCommand("0105"))
        val intake = parseTemp(sendCommand("010F"))
        val load = parsePercent(sendCommand("0104"))
        val map = parseMap(sendCommand("010B"))
        val tps = parsePercent(sendCommand("0111"))
        val voltage = parseVoltage(sendCommand("0142"))
        val ethanol = parseEthanol(sendCommand("0152"))
        val maf = parseMaf(sendCommand("0110"))
        val fuelPress = parseFuelPressure(sendCommand("010A"))

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
            fuelPressure = fuelPress
        )
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

    private fun parseDtcResponse(raw: String): List<String> {
        val clean = cleanHex(raw)
        // Procura 43XX...
        val idx = clean.indexOf("43")
        if (idx < 0) return emptyList()

        val codes = mutableListOf<String>()
        var i = idx + 2
        while (i + 4 <= clean.length) {
            val b1 = clean.substring(i, i + 2).toIntOrNull(16) ?: break
            val b2 = clean.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            if (b1 == 0 && b2 == 0) break

            val firstChar = when (b1 shr 6) {
                0 -> 'P'
                1 -> 'C'
                2 -> 'B'
                3 -> 'U'
                else -> 'P'
            }
            val code = "$firstChar${b1 and 0x3F}${String.format("%02X", b2)}"
            codes.add(code)
            i += 4
            if (codes.size >= 8) break // limite razoável
        }
        return codes
    }
}
