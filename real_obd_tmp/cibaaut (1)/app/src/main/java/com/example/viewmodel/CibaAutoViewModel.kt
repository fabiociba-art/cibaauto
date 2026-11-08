package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.obd.ObdManager
import com.example.car.TelemetryBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel real — SEM SIMULAÇÃO.
 * Só exibe dados vindos do adaptador OBD-2 físico.
 */
class CibaAutoViewModel : ViewModel() {

    private val obdManager = ObdManager()

    private val _selectedVehicle = MutableStateFlow<VehicleProfile>(PreloadedVehicles.corollaAltis2025)
    val selectedVehicle: StateFlow<VehicleProfile> = _selectedVehicle.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _connectionLogs = MutableStateFlow<List<String>>(
        listOf("Sistema CIBA AUTO inicializado. Aguardando conexão com adaptador OBD-2.")
    )
    val connectionLogs: StateFlow<List<String>> = _connectionLogs.asStateFlow()

    private val _diagnosticCodes = MutableStateFlow<List<DiagnosticCode>>(emptyList())
    val diagnosticCodes: StateFlow<List<DiagnosticCode>> = _diagnosticCodes.asStateFlow()

    private val _readinessMonitors = MutableStateFlow<Map<String, String>>(
        mapOf(
            "Misfire" to "—",
            "Fuel System" to "—",
            "Comprehensive Components" to "—",
            "Catalyst" to "—",
            "Evaporative System" to "—",
            "Oxygen Sensor" to "—",
            "EGR / VVT" to "—"
        )
    )
    val readinessMonitors: StateFlow<Map<String, String>> = _readinessMonitors.asStateFlow()

    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    private val _ethanolHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val ethanolHistory: StateFlow<List<Pair<String, Int>>> = _ethanolHistory.asStateFlow()

    /** Lista de dispositivos Bluetooth pareados */
    private val _btDevices = MutableStateFlow<List<ObdManager.BtDeviceInfo>>(emptyList())
    val btDevices: StateFlow<List<ObdManager.BtDeviceInfo>> = _btDevices.asStateFlow()

    /** Endereço MAC do dispositivo selecionado pelo usuário (null = auto) */
    private val _selectedBtAddress = MutableStateFlow<String?>(null)
    val selectedBtAddress: StateFlow<String?> = _selectedBtAddress.asStateFlow()

    private val _btEnabled = MutableStateFlow(false)
    val btEnabled: StateFlow<Boolean> = _btEnabled.asStateFlow()

    private var pollingJob: Job? = null

    init {
        // Nenhuma simulação. Apenas bridge para Android Auto.
        viewModelScope.launch {
            _telemetry.collect { state ->
                TelemetryBridge.update(state)
            }
        }
        viewModelScope.launch {
            _selectedVehicle.collect { v ->
                TelemetryBridge.updateVehicleName(v.fullName)
            }
        }
        // Carrega lista de dispositivos ao iniciar
        refreshBtDevices()
    }

    fun selectVehicle(vehicle: VehicleProfile) {
        _selectedVehicle.value = vehicle
        addLog("Veículo selecionado: ${vehicle.fullName}")
    }

    /** Atualiza lista de dispositivos Bluetooth pareados */
    fun refreshBtDevices() {
        viewModelScope.launch {
            _btEnabled.value = obdManager.isBluetoothEnabled()
            val list = obdManager.listPairedDevices()
            _btDevices.value = list
            addLog(
                if (list.isEmpty()) "Nenhum dispositivo Bluetooth pareado encontrado."
                else "Encontrados ${list.size} dispositivo(s) pareado(s)."
            )
            // Se nenhum selecionado ainda, pré-seleciona o primeiro "provável OBD"
            if (_selectedBtAddress.value == null) {
                list.firstOrNull { it.isLikelyObd }?.let {
                    _selectedBtAddress.value = it.address
                    addLog("Pré-selecionado: ${it.name} (${it.address})")
                }
            }
        }
    }

    fun selectBtDevice(address: String?) {
        _selectedBtAddress.value = address
        val name = _btDevices.value.firstOrNull { it.address == address }?.name ?: address
        addLog(if (address != null) "Dispositivo selecionado: $name" else "Seleção limpa (modo automático)")
    }

    /**
     * Conecta usando o dispositivo selecionado, ou tenta automático se nenhum estiver selecionado.
     */
    fun connectObd() {
        viewModelScope.launch {
            pollingJob?.cancel()
            _telemetry.update {
                it.copy(
                    connectionState = ConnectionState.SEARCHING,
                    isConnected = false,
                    isDemoMode = false
                )
            }

            val selected = _selectedBtAddress.value
            if (selected != null) {
                addLog("Iniciando conexão no dispositivo selecionado...")
            } else {
                addLog("Iniciando conexão OBD-2 (automático: Bluetooth → Wi-Fi)...")
            }

            _telemetry.update { it.copy(connectionState = ConnectionState.CONNECTING) }

            val success = if (selected != null) {
                obdManager.connectBluetoothToAddress(selected) { logMsg -> addLog(logMsg) }
            } else {
                obdManager.connectAuto { logMsg -> addLog(logMsg) }
            }

            if (success) {
                _telemetry.update {
                    it.copy(
                        connectionState = ConnectionState.CONNECTED,
                        isConnected = true,
                        isDemoMode = false
                    )
                }
                addLog("OBD-2 online. Iniciando telemetria real.")
                startPidPolling()
                refreshDiagnostics()
            } else {
                _telemetry.update {
                    it.copy(
                        connectionState = ConnectionState.ERROR,
                        isConnected = false,
                        isDemoMode = false
                    )
                }
                addLog("Falha na conexão. Verifique se o adaptador está ligado, pareado e selecionado.")
            }
        }
    }

    fun disconnectObd() {
        pollingJob?.cancel()
        obdManager.disconnect()
        _telemetry.update {
            TelemetryState(
                connectionState = ConnectionState.DISCONNECTED,
                isConnected = false,
                isDemoMode = false
            )
        }
        addLog("Desconectado do adaptador OBD-2.")
    }

    fun clearDtcCodes() {
        viewModelScope.launch {
            if (!obdManager.isConnected()) {
                addLog("Não conectado. Impossível limpar códigos.")
                return@launch
            }
            addLog("Enviando comando Mode 04 (Clear DTCs)...")
            val ok = obdManager.clearDtcs()
            if (ok) {
                _diagnosticCodes.value = emptyList()
                addLog("Códigos DTC apagados da ECU com sucesso.")
                val readiness = obdManager.readReadiness()
                if (readiness.isNotEmpty()) {
                    _readinessMonitors.value = readiness
                }
            } else {
                addLog("Falha ao limpar códigos. Verifique a conexão.")
            }
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            if (!obdManager.isConnected()) return@launch
            addLog("Lendo códigos de falha (Mode 03)...")
            val result = obdManager.readDtcs()
            val mapped = result.codes.map { code ->
                DiagnosticCode(
                    code = code,
                    title = dtcTitle(code),
                    description = dtcDescription(code),
                    system = dtcSystem(code),
                    severity = if (code.startsWith("P0") || code.startsWith("P2")) DTCLevel.WARNING else DTCLevel.INFO
                )
            }
            _diagnosticCodes.value = mapped
            addLog(
                if (mapped.isEmpty()) "Nenhum código de falha encontrado."
                else "${mapped.size} código(s) encontrado(s). MIL=${if (result.milOn) "ON" else "OFF"}"
            )

            val readiness = obdManager.readReadiness()
            if (readiness.isNotEmpty()) {
                _readinessMonitors.update { it + readiness }
            }
        }
    }

    private fun startPidPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && obdManager.isConnected()) {
                try {
                    val pids = obdManager.readMainPids()
                    _telemetry.update { current ->
                        current.copy(
                            rpm = pids.rpm ?: current.rpm,
                            speed = pids.speed ?: current.speed,
                            coolantTemp = pids.coolantTemp ?: current.coolantTemp,
                            intakeTemp = pids.intakeTemp ?: current.intakeTemp,
                            engineLoad = pids.engineLoad ?: current.engineLoad,
                            manifoldPressure = pids.manifoldPressure ?: current.manifoldPressure,
                            throttlePosition = pids.throttlePosition ?: current.throttlePosition,
                            batteryVoltage = pids.batteryVoltage ?: current.batteryVoltage,
                            ethanolPercentage = pids.ethanolPercentage ?: current.ethanolPercentage,
                            fuelPressure = pids.fuelPressure ?: current.fuelPressure,
                            fuelTypeLabel = pids.ethanolPercentage?.let { "Flex (Etanol E$it)" }
                                ?: current.fuelTypeLabel,
                            isConnected = true,
                            connectionState = ConnectionState.CONNECTED,
                            isDemoMode = false
                        )
                    }
                } catch (e: Exception) {
                    addLog("Erro na leitura de PIDs: ${e.message}")
                    if (!obdManager.isConnected()) {
                        _telemetry.update {
                            it.copy(
                                connectionState = ConnectionState.ERROR,
                                isConnected = false
                            )
                        }
                        addLog("Conexão perdida durante a leitura.")
                        break
                    }
                }
                delay(700)
            }
        }
    }

    private fun addLog(message: String) {
        _connectionLogs.update { (listOf(message) + it).take(80) }
    }

    private fun dtcTitle(code: String): String = when {
        code.startsWith("P01") -> "Sistema de Combustível / Sensores"
        code.startsWith("P02") -> "Injetores / Combustível"
        code.startsWith("P03") -> "Ignição / Misfire"
        code.startsWith("P04") -> "Controle de Emissões"
        code.startsWith("P05") -> "Velocidade / Controle de Marcha Lenta"
        code.startsWith("P06") -> "Circuito do Computador"
        code.startsWith("C") -> "Chassis / ABS / Tração"
        code.startsWith("B") -> "Carroceria"
        code.startsWith("U") -> "Rede / Comunicação"
        else -> "Código de Diagnóstico"
    }

    private fun dtcDescription(code: String): String = when (code) {
        "P0130", "P0131", "P0132", "P0133", "P0134" -> "Circuito do sensor de oxigênio (Banco 1 Sensor 1)."
        "P0420" -> "Eficiência do catalisador abaixo do limiar (Banco 1)."
        "P0171" -> "Sistema muito pobre (Banco 1)."
        "P0172" -> "Sistema muito rico (Banco 1)."
        "P0300" -> "Falha de ignição aleatória / múltiplos cilindros."
        "P0301", "P0302", "P0303", "P0304" -> "Falha de ignição no cilindro específico."
        else -> "Consulte o manual da ECU ou scanner avançado para detalhes do código $code."
    }

    private fun dtcSystem(code: String): String = when {
        code.startsWith("P0") || code.startsWith("P2") -> "Powertrain"
        code.startsWith("C") -> "Chassis"
        code.startsWith("B") -> "Body"
        code.startsWith("U") -> "Network"
        else -> "Desconhecido"
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        obdManager.disconnect()
    }
}
