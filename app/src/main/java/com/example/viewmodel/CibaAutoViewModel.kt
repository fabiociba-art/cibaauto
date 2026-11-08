package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel real — SEM SIMULAÇÃO.
 * Só exibe dados vindos do adaptador OBD-2 físico.
 */
class CibaAutoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<Application>().getSharedPreferences("ciba_auto_prefs", Context.MODE_PRIVATE)

    private val obdManager = ObdManager()

    private val _vehicles = MutableStateFlow<List<VehicleProfile>>(loadVehicles())
    val vehicles: StateFlow<List<VehicleProfile>> = _vehicles.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<VehicleProfile>(loadInitialSelectedVehicle())
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

    private val _vehicleInfo = MutableStateFlow<ObdManager.VehicleInfo?>(null)
    val vehicleInfo: StateFlow<ObdManager.VehicleInfo?> = _vehicleInfo.asStateFlow()

    private val _vehicleInfoLoading = MutableStateFlow(false)
    val vehicleInfoLoading: StateFlow<Boolean> = _vehicleInfoLoading.asStateFlow()

    private val _history = MutableStateFlow(ChartHistory())
    val history: StateFlow<ChartHistory> = _history.asStateFlow()

    private val _isHybridPowerInverted = MutableStateFlow(prefs.getBoolean("invert_hybrid_power_sign", false))
    val isHybridPowerInverted: StateFlow<Boolean> = _isHybridPowerInverted.asStateFlow()

    fun toggleInvertHybridPowerSign() {
        val newVal = !_isHybridPowerInverted.value
        _isHybridPowerInverted.value = newVal
        prefs.edit().putBoolean("invert_hybrid_power_sign", newVal).apply()
        addLog(if (newVal) "Inversão de sinal da potência híbrida ATIVADA." else "Inversão de sinal da potência híbrida DESATIVADA.")
    }

    private var demoJob: Job? = null

    /** Alterna o modo de demonstração simulado com ciclo híbrido completo. */
    fun toggleDemoMode() {
        if (_telemetry.value.isDemoMode) {
            stopDemoMode()
        } else {
            startDemoMode()
        }
    }

    fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
        _telemetry.update {
            it.copy(
                isDemoMode = false,
                isConnected = false,
                connectionState = ConnectionState.DISCONNECTED
            )
        }
        com.example.service.ObdForegroundService.stop(getApplication())
        addLog("Modo demonstração finalizado.")
    }

    fun startDemoMode() {
        pollingJob?.cancel()
        obdManager.disconnect()
        demoJob?.cancel()

        com.example.service.ObdForegroundService.start(getApplication())
        addLog("Modo demonstração ativado: ciclo STANDBY → EV → HÍBRIDO → COMBUSTÃO → CARREGANDO.")
        demoJob = viewModelScope.launch {
            var cycleSeconds = 0f
            var simSoc = 62
            var simTripKm = 4.2f
            var simOdo = 12450f

            while (isActive) {
                val period = 32f // ciclo completo de 32 segundos
                val phase = cycleSeconds % period

                val curSpeed: Float
                val curRpm: Int
                val curPowerKw: Float
                val curLoad: Int
                val curThrottle: Int
                val curFuelFlow: Float
                val curInstantKml: Float

                when {
                    // 1. STANDBY (0s .. 5s): Parado em READY, motor a combustão desligado, potência zero
                    phase < 5f -> {
                        curSpeed = 0f
                        curRpm = 0
                        curPowerKw = 0f
                        curLoad = 0
                        curThrottle = 0
                        curFuelFlow = 0f
                        curInstantKml = 0f
                    }
                    // 2. EV (5s .. 11s): Saída 100% elétrica (bateria -> rodas)
                    phase < 11f -> {
                        val progress = (phase - 5f) / 6f
                        curSpeed = 6f + progress * 32f // 6 -> 38 km/h
                        curRpm = 0 // Motor térmico desligado
                        curPowerKw = 6f + kotlin.math.sin(progress * Math.PI.toFloat()) * 14f // 6 -> 20 kW
                        curLoad = 18
                        curThrottle = (progress * 28f).toInt()
                        curFuelFlow = 0f
                        curInstantKml = 40f
                        if (progress > 0.8f && simSoc > 55) simSoc = 61
                    }
                    // 3. HÍBRIDO (11s .. 19s): Aceleração combinada (combustão + bateria -> rodas)
                    phase < 19f -> {
                        val progress = (phase - 11f) / 8f
                        curSpeed = 38f + progress * 52f // 38 -> 90 km/h
                        curRpm = (1300 + progress * 2100).toInt() // 1300 -> 3400 RPM
                        curPowerKw = 12f + kotlin.math.sin(progress * Math.PI.toFloat()) * 26f // 12 -> 38 kW
                        curLoad = (45 + progress * 40).toInt()
                        curThrottle = (40 + progress * 42).toInt()
                        curFuelFlow = 3.2f + progress * 3.4f
                        curInstantKml = 17.2f
                        if (progress > 0.6f) simSoc = 60
                    }
                    // 4. COMBUSTÃO (19s .. 25s): Cruzeiro constante em rodovia (motor térmico -> rodas)
                    phase < 25f -> {
                        val progress = (phase - 19f) / 6f
                        curSpeed = 88f + kotlin.math.sin(progress * Math.PI.toFloat() * 2f) * 2f // ~90 km/h
                        curRpm = 1720 + (kotlin.math.sin(progress * 4f) * 40).toInt()
                        curPowerKw = 0.6f // Elétrico apenas flutuando em standby
                        curLoad = 35
                        curThrottle = 30
                        curFuelFlow = 4.1f
                        curInstantKml = 22.0f
                    }
                    // 5. CARREGANDO / REGENERAÇÃO (25s .. 32s): Frenagem regenerativa (rodas -> motor/gerador -> bateria)
                    else -> {
                        val progress = (phase - 25f) / 7f
                        curSpeed = (90f - progress * 90f).coerceAtLeast(0f) // 90 -> 0 km/h
                        curRpm = 0 // Injeção cortada
                        curPowerKw = -14f - kotlin.math.sin(progress * Math.PI.toFloat()) * 22f // -14 -> -36 kW (recarga!)
                        curLoad = 0
                        curThrottle = 0
                        curFuelFlow = 0f
                        curInstantKml = 40f
                        if (progress > 0.4f) simSoc = 63 // Bateria carregada pela regeneração!
                    }
                }

                val dt = 0.25f
                simTripKm += (curSpeed * (dt / 3600f))
                simOdo += (curSpeed * (dt / 3600f))

                _telemetry.update { prev ->
                    prev.copy(
                        speed = curSpeed,
                        rpm = curRpm,
                        hybridMotorPowerKw = curPowerKw,
                        hybridBatterySoc = simSoc,
                        isHybridDetected = true,
                        engineLoad = curLoad,
                        throttlePosition = curThrottle,
                        fuelFlowLitersPerHour = curFuelFlow,
                        instantConsumption = curInstantKml,
                        averageConsumption = 22.4f,
                        rangeKm = 740,
                        batteryVoltage = 13.8f,
                        coolantTemp = 89,
                        intakeTemp = 27,
                        ethanolPercentage = 27,
                        fuelTypeLabel = "Gasolina E27",
                        odometerKm = simOdo,
                        tripDistanceKm = simTripKm,
                        tripDurationSec = prev.tripDurationSec + 1,
                        tripAvgSpeed = 52.0f,
                        tripMaxSpeed = 90f,
                        isConnected = true,
                        connectionState = ConnectionState.CONNECTED,
                        isDemoMode = true
                    )
                }

                val t = _telemetry.value
                _history.update { h ->
                    ChartHistory(
                        rpm = (h.rpm + t.rpm.toFloat()).takeLast(120),
                        speed = (h.speed + t.speed).takeLast(120),
                        load = (h.load + t.engineLoad.toFloat()).takeLast(120),
                        coolant = (h.coolant + t.coolantTemp.toFloat()).takeLast(120)
                    )
                }

                cycleSeconds += 0.25f
                delay(250)
            }
        }
    }

    private var tripSeconds: Float = 0f
    private var tripMaxSpeed: Float = 0f

    /** Zera distância, combustível, tempo e velocidade máxima da viagem atual. */
    fun resetTrip() {
        accumulatedTripKm = 0f
        accumulatedFuelLiters = 0f
        tripSeconds = 0f
        tripMaxSpeed = 0f
        _telemetry.update {
            it.copy(
                tripDistanceKm = 0f,
                totalFuelConsumedLiters = 0f,
                tripDurationSec = 0,
                tripMaxSpeed = 0f,
                tripAvgSpeed = 0f,
                averageConsumption = 0f
            )
        }
        addLog("Viagem reiniciada.")
    }

    /** Lê VIN, calibração e tipo de combustível (pausa o polling durante a leitura). */
    fun readVehicleInfo() {
        if (_vehicleInfoLoading.value) return
        viewModelScope.launch {
            if (!obdManager.isConnected()) {
                addLog("Conecte ao OBD para ler as informações do veículo.")
                return@launch
            }
            _vehicleInfoLoading.value = true
            pollingJob?.cancel()
            pollingJob?.join()
            try {
                _vehicleInfo.value = obdManager.readVehicleInfo()
            } catch (e: Exception) {
                addLog("Erro ao ler informações do veículo: ${e.message}")
            } finally {
                _vehicleInfoLoading.value = false
                if (obdManager.isConnected()) startPidPolling()
            }
        }
    }

    private val _hybridScanReport = MutableStateFlow<List<String>>(emptyList())
    val hybridScanReport: StateFlow<List<String>> = _hybridScanReport.asStateFlow()

    private val _hybridScanRunning = MutableStateFlow(false)
    val hybridScanRunning: StateFlow<Boolean> = _hybridScanRunning.asStateFlow()

    /** Varredura somente-leitura do sistema híbrido. Pausa o polling enquanto roda (o barramento é compartilhado). */
    fun runHybridScan() {
        if (_hybridScanRunning.value) return
        viewModelScope.launch {
            if (!obdManager.isConnected()) {
                _hybridScanReport.value = listOf("Conecte ao adaptador OBD primeiro (carro ligado em READY).")
                return@launch
            }
            _hybridScanRunning.value = true
            _hybridScanReport.value = emptyList()
            pollingJob?.cancel()
            pollingJob?.join()
            try {
                obdManager.probeHybrid { line -> _hybridScanReport.update { it + line } }
            } catch (e: Exception) {
                _hybridScanReport.update { it + "Erro na varredura: ${e.message}" }
            } finally {
                _hybridScanRunning.value = false
                if (obdManager.isConnected()) startPidPolling()
            }
        }
    }

    private var pollingJob: Job? = null
    private var lastPollTimestampMs: Long = 0L
    private var accumulatedTripKm: Float = 0f
    private var accumulatedFuelLiters: Float = 0f

    init {
        TelemetryBridge.registerCallbacks(
            onConnect = { connectObd() },
            onDisconnect = { disconnectObd() },
            onToggleDemo = { toggleDemoMode() },
            onClearDtc = { clearDtcCodes() },
            onRefreshDiagnostics = { refreshDiagnostics() },
            onResetTrip = { resetTrip() },
            onSelectVehicle = { v -> selectVehicle(v) },
            onTogglePowerInvert = { toggleInvertHybridPowerSign() },
            onSelectBtDevice = { addr -> selectBtDevice(addr) },
            onRefreshBtDevices = { refreshBtDevices() },
            onRunHybridScan = { runHybridScan() },
            onReadVehicleInfo = { readVehicleInfo() }
        )
        TelemetryBridge.updatePowerInverted(_isHybridPowerInverted.value)

        viewModelScope.launch {
            _telemetry.collect { state ->
                TelemetryBridge.update(state)
                TelemetryBridge.updateAlerts(com.example.model.AlertEvaluator.evaluate(state))
            }
        }
        viewModelScope.launch {
            _selectedVehicle.collect { v ->
                TelemetryBridge.updateVehicles(_vehicles.value, v)
            }
        }
        viewModelScope.launch {
            _vehicles.collect { list ->
                TelemetryBridge.updateVehicles(list, _selectedVehicle.value)
            }
        }
        viewModelScope.launch {
            _isHybridPowerInverted.collect { inv ->
                TelemetryBridge.updatePowerInverted(inv)
            }
        }
        viewModelScope.launch {
            _diagnosticCodes.collect { codes ->
                TelemetryBridge.updateDiagnostics(codes, _readinessMonitors.value)
            }
        }
        viewModelScope.launch {
            _readinessMonitors.collect { monitors ->
                TelemetryBridge.updateDiagnostics(_diagnosticCodes.value, monitors)
            }
        }
        viewModelScope.launch {
            _trips.collect { trips ->
                TelemetryBridge.updateTrips(trips)
            }
        }
        viewModelScope.launch {
            _ethanolHistory.collect { history ->
                TelemetryBridge.updateEthanolHistory(history)
            }
        }
        viewModelScope.launch {
            _btDevices.collect { devs ->
                TelemetryBridge.updateBluetooth(devs, _selectedBtAddress.value)
            }
        }
        viewModelScope.launch {
            _selectedBtAddress.collect { addr ->
                TelemetryBridge.updateBluetooth(_btDevices.value, addr)
            }
        }
        viewModelScope.launch {
            _vehicleInfo.collect { info ->
                TelemetryBridge.updateVehicleInfo(info, _vehicleInfoLoading.value)
            }
        }
        viewModelScope.launch {
            _vehicleInfoLoading.collect { loading ->
                TelemetryBridge.updateVehicleInfo(_vehicleInfo.value, loading)
            }
        }
        viewModelScope.launch {
            _hybridScanReport.collect { report ->
                TelemetryBridge.updateHybridScan(report, _hybridScanRunning.value)
            }
        }
        viewModelScope.launch {
            _hybridScanRunning.collect { running ->
                TelemetryBridge.updateHybridScan(_hybridScanReport.value, running)
            }
        }
        // Carrega lista de dispositivos ao iniciar
        refreshBtDevices()
    }

    /**
     * A lista salva no aparelho ("custom_vehicles_json") tem prioridade sobre os perfis de fábrica,
     * então perfis novos nunca apareciam para quem já abriu o app. Aqui, perfis de fábrica ainda
     * não "semeados" (ids em seeded_vehicle_ids) são acrescentados uma única vez — sem recriar
     * os que o usuário excluiu depois.
     */
    private fun loadVehicles(): List<VehicleProfile> {
        val seeded = prefs.getStringSet("seeded_vehicle_ids", emptySet()) ?: emptySet()
        val raw = prefs.getString("custom_vehicles_json", null)
        var list: MutableList<VehicleProfile>? = null
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val parsed = mutableListOf<VehicleProfile>()
                for (i in 0 until array.length()) {
                    parsed.add(vehicleProfileFromJson(array.getJSONObject(i)))
                }
                if (parsed.isNotEmpty()) list = parsed
            } catch (_: Exception) {}
        }
        if (list == null) {
            // Primeira execução (ou lista inválida): tudo de fábrica já conta como semeado.
            prefs.edit().putStringSet("seeded_vehicle_ids", PreloadedVehicles.all.map { it.id }.toSet()).apply()
            return PreloadedVehicles.all
        }
        val current: MutableList<VehicleProfile> = list
        // Instalações antigas ainda não tinham o controle de semeadura: os 3 perfis originais já foram vistos.
        val alreadySeen = if (seeded.isEmpty()) {
            setOf("corolla_altis_2025", "corolla_hybrid_2025", "yaris_xl_plus_2025")
        } else seeded
        val missing = PreloadedVehicles.all.filter { it.id !in alreadySeen && current.none { v -> v.id == it.id } }
        if (missing.isNotEmpty()) {
            current.addAll(0, missing)
            val array = JSONArray()
            current.forEach { array.put(it.toJsonObject()) }
            prefs.edit()
                .putString("custom_vehicles_json", array.toString())
                .putStringSet("seeded_vehicle_ids", PreloadedVehicles.all.map { it.id }.toSet())
                .apply()
        } else if (seeded.isEmpty()) {
            prefs.edit().putStringSet("seeded_vehicle_ids", PreloadedVehicles.all.map { it.id }.toSet()).apply()
        }
        return current
    }

    private fun loadInitialSelectedVehicle(): VehicleProfile {
        val list = loadVehicles()
        var selectedId = prefs.getString("selected_vehicle_id", null)
        // Seleciona o Corolla GLi Hybrid 2026 uma vez (o carro do usuário).
        if (!prefs.getBoolean("gli_hybrid_2026_selected_once", false)) {
            list.firstOrNull { it.id == PreloadedVehicles.corollaGliHybrid2026.id }?.let {
                selectedId = it.id
                prefs.edit().putString("selected_vehicle_id", it.id).putBoolean("gli_hybrid_2026_selected_once", true).apply()
            }
        }
        return list.firstOrNull { it.id == selectedId } ?: list.firstOrNull() ?: PreloadedVehicles.corollaGliHybrid2026
    }

    private fun persistVehicles(list: List<VehicleProfile>, selectedId: String?) {
        val array = JSONArray()
        list.forEach { array.put(it.toJsonObject()) }
        prefs.edit()
            .putString("custom_vehicles_json", array.toString())
            .putString("selected_vehicle_id", selectedId ?: _selectedVehicle.value.id)
            .apply()
    }

    fun selectVehicle(vehicle: VehicleProfile) {
        _selectedVehicle.value = vehicle
        prefs.edit().putString("selected_vehicle_id", vehicle.id).apply()
        addLog("Veículo selecionado: ${vehicle.fullName}")
    }

    fun saveVehicle(profile: VehicleProfile) {
        val current = _vehicles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
            addLog("Perfil de veículo atualizado: ${profile.fullName}")
        } else {
            current.add(0, profile)
            addLog("Novo perfil de veículo criado: ${profile.fullName}")
        }
        _vehicles.value = current
        if (_selectedVehicle.value.id == profile.id || current.size == 1) {
            _selectedVehicle.value = profile
        }
        persistVehicles(current, _selectedVehicle.value.id)
    }

    fun deleteVehicle(vehicleId: String) {
        val current = _vehicles.value.toMutableList()
        val removed = current.removeAll { it.id == vehicleId }
        if (removed) {
            if (current.isEmpty()) {
                current.addAll(PreloadedVehicles.all)
            }
            _vehicles.value = current
            if (_selectedVehicle.value.id == vehicleId) {
                val newSelected = current.first()
                _selectedVehicle.value = newSelected
                addLog("Veículo ativo alterado para: ${newSelected.fullName}")
            }
            persistVehicles(current, _selectedVehicle.value.id)
            addLog("Perfil excluído com sucesso.")
        }
    }

    fun resetVehiclesToDefault() {
        _vehicles.value = PreloadedVehicles.all
        _selectedVehicle.value = PreloadedVehicles.corollaGliHybrid2026
        prefs.edit()
            .remove("custom_vehicles_json")
            .putStringSet("seeded_vehicle_ids", PreloadedVehicles.all.map { it.id }.toSet())
            .putString("selected_vehicle_id", PreloadedVehicles.corollaGliHybrid2026.id)
            .apply()
        addLog("Perfis restaurados para os padrões originais de fábrica.")
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
            demoJob?.cancel()
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
                com.example.service.ObdForegroundService.start(getApplication())
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
        demoJob?.cancel()
        pollingJob?.cancel()
        obdManager.disconnect()
        com.example.service.ObdForegroundService.stop(getApplication())
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
        lastPollTimestampMs = System.currentTimeMillis()
        pollingJob = viewModelScope.launch {
            while (isActive && obdManager.isConnected()) {
                try {
                    val isHybridVehicle = _selectedVehicle.value.isHybrid
                    val pids = obdManager.readMainPids(queryHybrid = isHybridVehicle)

                    val now = System.currentTimeMillis()
                    val dtSeconds = if (lastPollTimestampMs > 0L) {
                        ((now - lastPollTimestampMs) / 1000f).coerceIn(0.1f, 5.0f)
                    } else 0.7f
                    lastPollTimestampMs = now
                    tripSeconds += dtSeconds
                    (pids.speed ?: 0f).let { if (it > tripMaxSpeed) tripMaxSpeed = it }

                    _telemetry.update { current ->
                        val curRpm = pids.rpm ?: current.rpm
                        val curSpeed = pids.speed ?: current.speed
                        val curEthanol = pids.ethanolPercentage ?: current.ethanolPercentage

                        // 1. Integração real de Odômetro e Distância da Viagem
                        val deltaDistanceKm = if (curSpeed > 0f) {
                            (curSpeed * (dtSeconds / 3600f))
                        } else 0f
                        accumulatedTripKm += deltaDistanceKm

                        val effectiveOdometerKm = if (pids.odometerKm != null && pids.odometerKm > 0f) {
                            pids.odometerKm
                        } else {
                            accumulatedTripKm
                        }

                        // 2. Cálculo real de Consumo de Combustível (MAF ou Speed-Density)
                        val r = (curEthanol / 100f).coerceIn(0f, 1f)
                        val afr = 14.7f * (1f - r) + 9.0f * r
                        val density = 745f * (1f - r) + 789f * r

                        val mafGps = if (pids.maf != null && pids.maf > 0f) {
                            pids.maf
                        } else {
                            // Método Speed-Density alternativo baseado no MAP + IAT + RPM
                            val displacementL = _selectedVehicle.value.getDisplacementLiters()
                            val curMap = pids.manifoldPressure ?: current.manifoldPressure.coerceAtLeast(30f)
                            val curIat = pids.intakeTemp ?: current.intakeTemp.coerceAtLeast(15)
                            val imap = (curRpm * curMap) / (curIat + 273.15f)
                            (imap / 120f) * 0.82f * displacementL * (28.97f / 8.314f)
                        }

                        val fuelFlowLph = if (curRpm > 250) {
                            ((mafGps / (afr * density)) * 3600f).coerceIn(0.2f, 45f)
                        } else 0f

                        val deltaFuelLiters = fuelFlowLph * (dtSeconds / 3600f)
                        accumulatedFuelLiters += deltaFuelLiters

                        // Consumo Instantâneo (km/L)
                        val instantKml = if (curSpeed > 2f && fuelFlowLph > 0.1f) {
                            (curSpeed / fuelFlowLph).coerceIn(0f, 40f)
                        } else 0f

                        // Consumo Médio (km/L) acumulado
                        val avgKml = if (accumulatedTripKm > 0.05f && accumulatedFuelLiters > 0.005f) {
                            (accumulatedTripKm / accumulatedFuelLiters).coerceIn(2f, 35f)
                        } else if (instantKml > 0f) {
                            instantKml
                        } else {
                            current.averageConsumption
                        }

                        // 3. Autonomia real (Range em KM)
                        val tankCap = _selectedVehicle.value.getTankCapacityLiters()
                        val fuelLevelPct = pids.fuelLevelPercent ?: current.fuelLevelPercent
                        val litersRemaining = if (fuelLevelPct != null) {
                            (tankCap * (fuelLevelPct / 100f)).coerceAtLeast(1f)
                        } else {
                            (tankCap * 0.70f - accumulatedFuelLiters).coerceAtLeast(2f)
                        }
                        val effAvg = if (avgKml > 3f) avgKml else if (instantKml > 3f) instantKml else 12.0f
                        val calculatedRange = (litersRemaining * effAvg).toInt().coerceIn(10, 1200)

                        current.copy(
                            rpm = curRpm,
                            speed = curSpeed,
                            coolantTemp = pids.coolantTemp ?: current.coolantTemp,
                            intakeTemp = pids.intakeTemp ?: current.intakeTemp,
                            engineLoad = pids.engineLoad ?: current.engineLoad,
                            manifoldPressure = pids.manifoldPressure ?: current.manifoldPressure,
                            throttlePosition = pids.throttlePosition ?: current.throttlePosition,
                            batteryVoltage = pids.batteryVoltage ?: current.batteryVoltage,
                            ethanolPercentage = curEthanol,
                            fuelPressure = pids.fuelPressure ?: current.fuelPressure,
                            fuelTypeLabel = if (curEthanol > 0) "${fuelBlendLabel(curEthanol)} • E$curEthanol" else current.fuelTypeLabel,
                            odometerKm = effectiveOdometerKm,
                            tripDistanceKm = accumulatedTripKm,
                            fuelFlowLitersPerHour = fuelFlowLph,
                            totalFuelConsumedLiters = accumulatedFuelLiters,
                            instantConsumption = instantKml,
                            averageConsumption = avgKml,
                            rangeKm = calculatedRange,
                            fuelLevelPercent = fuelLevelPct,
                            hybridBatterySoc = pids.hybridBatterySoc ?: current.hybridBatterySoc,
                            hybridMotorPowerKw = pids.hybridMotorPowerKw ?: current.hybridMotorPowerKw,
                            isHybridDetected = pids.isHybridDetected || current.isHybridDetected,
                            oilTemp = pids.oilTemp ?: current.oilTemp,
                            ambientTemp = pids.ambientTemp ?: current.ambientTemp,
                            catalystTemp = pids.catalystTemp ?: current.catalystTemp,
                            tripDurationSec = tripSeconds.toInt(),
                            tripMaxSpeed = tripMaxSpeed,
                            tripAvgSpeed = if (tripSeconds > 5f) accumulatedTripKm / (tripSeconds / 3600f) else 0f,
                            hybridExtras = if (pids.hybridExtras.isNotEmpty()) current.hybridExtras + pids.hybridExtras else current.hybridExtras,
                            isConnected = true,
                            connectionState = ConnectionState.CONNECTED,
                            isDemoMode = false
                        )
                    }
                    val t = _telemetry.value
                    _history.update { h ->
                        ChartHistory(
                            rpm = (h.rpm + t.rpm.toFloat()).takeLast(120),
                            speed = (h.speed + t.speed).takeLast(120),
                            load = (h.load + t.engineLoad.toFloat()).takeLast(120),
                            coolant = (h.coolant + t.coolantTemp.toFloat()).takeLast(120)
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
                delay(650)
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
        demoJob?.cancel()
        pollingJob?.cancel()
        obdManager.disconnect()
        com.example.service.ObdForegroundService.stop(getApplication())
    }
}
