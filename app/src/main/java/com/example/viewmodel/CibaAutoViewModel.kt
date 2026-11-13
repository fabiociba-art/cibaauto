package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BluetoothDeviceItem
import com.example.model.DiagnosticsState
import com.example.model.DriveMode
import com.example.model.GaugeCatalog
import com.example.model.DtcKind
import com.example.model.dtcEntryOf
import com.example.model.ObdConnectionState
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import com.example.model.VehicleProfiles
import com.example.model.toJsonObject
import com.example.model.vehicleProfileFromJson
import org.json.JSONArray
import com.example.obd.ObdManager
import com.example.obd.ObdSimulator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    HYBRID_FLOW,
    DIAGNOSTICS,
    SETTINGS
}

class CibaAutoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val obdManager = ObdManager()
    private val obdSimulator = ObdSimulator()

    private val _telemetry = MutableStateFlow(blankTelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _allProfiles = MutableStateFlow(loadInitialProfiles())
    val allProfiles: StateFlow<List<VehicleProfile>> = _allProfiles.asStateFlow()

    // Perfil salvo da última vez; se não houver, usa o Corolla GLi Hybrid 2026
    private val _selectedProfile = MutableStateFlow(
        prefs.getString(KEY_PROFILE_ID, null)
            ?.let { id -> _allProfiles.value.firstOrNull { it.id == id } }
            ?: VehicleProfiles.DEFAULT
    )
    val selectedProfile: StateFlow<VehicleProfile> = _selectedProfile.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false) // o app abre em modo real (OBD); demo só se o usuário ligar
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // ---- Manômetros personalizáveis (sensor de cada posição, salvo entre aberturas do app) ----
    private val _gaugeSlots = MutableStateFlow(loadGaugeSlots())
    val gaugeSlots: StateFlow<List<String>> = _gaugeSlots.asStateFlow()

    val connectionState: StateFlow<ObdConnectionState> = obdManager.connectionState

    // Endereço do adaptador que está conectando agora (só ele mostra "Conectando..." na lista)
    private val _connectingAddress = MutableStateFlow<String?>(null)
    val connectingAddress: StateFlow<String?> = _connectingAddress.asStateFlow()

    // true depois que chegou a primeira telemetria da conexão atual (antes disso o ecuLinkOk é falso só por falta de dados)
    private val _liveDataReceived = MutableStateFlow(false)
    val liveDataReceived: StateFlow<Boolean> = _liveDataReceived.asStateFlow()
    val connectedDevice: StateFlow<BluetoothDeviceItem?> = obdManager.connectedDevice
    val diagnosticLogs: StateFlow<List<String>> = obdManager.diagnosticLogs

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

    // ---- Diagnóstico ----
    private val _demoDiagnostics = MutableStateFlow(demoDiagnosticsSample())

    /** Real (adaptador OBD) ou simulado (modo demo). */
    val diagnostics: StateFlow<DiagnosticsState> = combine(
        _isDemoMode, obdManager.diagnostics, _demoDiagnostics
    ) { demo, real, fake ->
        if (demo) fake.copy(isDemo = true) else real
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DiagnosticsState())

    init {
        // Limpa o "conectando" quando a conexão sai desse estado (conectou ou falhou)
        viewModelScope.launch {
            obdManager.connectionState.collect { state ->
                if (state != ObdConnectionState.CONNECTING) _connectingAddress.value = null
            }
        }
        applyGaugePriority()
        refreshDevices()
        startTelemetrySource()
        autoConnectLastDevice()
    }

    fun setGaugeSlot(index: Int, sensorId: String) {
        if (index !in 0 until GaugeCatalog.SLOT_COUNT || sensorId !in GaugeCatalog.byId) return
        val updated = _gaugeSlots.value.toMutableList()
        updated[index] = sensorId
        saveGaugeSlots(updated)
    }

    fun resetGaugeSlots() {
        saveGaugeSlots(GaugeCatalog.DEFAULT_SLOTS)
    }

    private fun loadGaugeSlots(): List<String> =
        GaugeCatalog.sanitize(prefs.getString(KEY_GAUGE_SLOTS, null)?.split(","))

    private fun saveGaugeSlots(slots: List<String>) {
        _gaugeSlots.value = slots
        prefs.edit().putString(KEY_GAUGE_SLOTS, slots.joinToString(",")).apply()
        applyGaugePriority()
    }

    /** Sensores adicionais que estão em algum manômetro passam a ser lidos com prioridade. */
    private fun applyGaugePriority() {
        val pids = _gaugeSlots.value
            .flatMap { GaugeCatalog.byId[it]?.pids ?: emptySet() }
            .toSet()
        obdManager.setPriorityPids(pids)
    }

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectProfile(profile: VehicleProfile) {
        _selectedProfile.value = profile
        prefs.edit().putString(KEY_PROFILE_ID, profile.id).apply()
        // Conexão OBD ativa: atualiza o perfil sem reconectar. Demo: reinicia o simulador.
        obdManager.updateProfile(profile)
        if (_isDemoMode.value) {
            startTelemetrySource()
        } else {
            val cur = _telemetry.value
            val effectiveTank = if (profile.tankCapacityLiters > 0.0) profile.tankCapacityLiters else 50.0
            val liters = (effectiveTank * (cur.fuelLevelPercent / 100.0)).toFloat()
            _telemetry.value = cur.copy(fuelLiters = liters)
        }
    }

    fun updateProfile(updated: VehicleProfile) {
        val list = _allProfiles.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
        } else {
            list.add(updated)
        }
        _allProfiles.value = list
        if (_selectedProfile.value.id == updated.id) {
            _selectedProfile.value = updated
            obdManager.updateProfile(updated)
            if (_isDemoMode.value) {
                startTelemetrySource()
            } else {
                val cur = _telemetry.value
                val effectiveTank = if (updated.tankCapacityLiters > 0.0) updated.tankCapacityLiters else 50.0
                val liters = (effectiveTank * (cur.fuelLevelPercent / 100.0)).toFloat()
                _telemetry.value = cur.copy(fuelLiters = liters)
            }
        }
        saveCustomProfiles()
    }

    private fun loadInitialProfiles(): List<VehicleProfile> {
        val baseProfiles = VehicleProfiles.DEFAULT_PROFILES.toMutableList()
        val customJson = prefs.getString(KEY_CUSTOM_PROFILES, null) ?: return baseProfiles
        return try {
            val array = JSONArray(customJson)
            for (i in 0 until array.length()) {
                val p = vehicleProfileFromJson(array.getJSONObject(i))
                val idx = baseProfiles.indexOfFirst { it.id == p.id }
                if (idx >= 0) {
                    baseProfiles[idx] = p
                } else {
                    baseProfiles.add(p)
                }
            }
            baseProfiles
        } catch (_: Exception) {
            baseProfiles
        }
    }

    private fun saveCustomProfiles() {
        try {
            val array = JSONArray()
            for (p in _allProfiles.value) {
                val original = VehicleProfiles.DEFAULT_PROFILES.firstOrNull { it.id == p.id }
                if (original == null || original != p) {
                    array.put(p.toJsonObject())
                }
            }
            prefs.edit().putString(KEY_CUSTOM_PROFILES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        _liveDataReceived.value = false
        _connectingAddress.value = null
        if (enabled) {
            _demoDiagnostics.value = demoDiagnosticsSample()
            obdManager.disconnect()
            startTelemetrySource()
        } else {
            // Saiu do demo: para o simulador e zera os valores falsos até conectar ao adaptador
            obdSimulator.stop()
            _telemetry.value = blankTelemetryData()
            autoConnectLastDevice()
        }
    }

    fun refreshDevices() {
        _pairedDevices.value = obdManager.getPairedDevices()
    }

    fun connectDevice(device: BluetoothDeviceItem) {
        _isDemoMode.value = false
        obdSimulator.stop()
        prefs.edit().putString(KEY_LAST_DEVICE, device.address).apply()
        _liveDataReceived.value = false
        _connectingAddress.value = device.address
        obdManager.connectToDevice(
            deviceItem = device,
            scope = viewModelScope,
            profile = _selectedProfile.value
        ) { incoming ->
            _liveDataReceived.value = true
            // O modo (ECO/NORMAL/SPORT) vem sempre da leitura da ECU; o usuário não escolhe.
            // A força G vem do acelerômetro do celular, não do OBD: preserva o valor atual
            _telemetry.value = incoming.copy(
                lateralGForce = _telemetry.value.lateralGForce
            )
        }
    }

    /** Chamado quando o usuário concede a permissão de Bluetooth (Android 12+). */
    fun onBluetoothPermissionGranted() {
        refreshDevices()
        autoConnectLastDevice()
    }

    /** Reconecta sozinho ao último adaptador OBD usado (se estiver pareado e o app não estiver em demo). */
    fun autoConnectLastDevice() {
        if (_isDemoMode.value) return
        if (obdManager.connectionState.value != ObdConnectionState.DISCONNECTED) return
        if (!hasBluetoothPermission()) return
        val lastAddress = prefs.getString(KEY_LAST_DEVICE, null) ?: return
        val device = obdManager.getPairedDevices().firstOrNull { it.address == lastAddress } ?: return
        connectDevice(device)
    }

    private fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                getApplication<Application>(), Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

    fun scanDtcs() {
        if (_isDemoMode.value) {
            viewModelScope.launch {
                _demoDiagnostics.update { it.copy(isScanning = true, message = null) }
                delay(900)
                _demoDiagnostics.update { it.copy(isScanning = false, lastScanMs = System.currentTimeMillis()) }
            }
        } else {
            obdManager.requestScan()
        }
    }

    fun clearDtcs() {
        if (_isDemoMode.value) {
            viewModelScope.launch {
                _demoDiagnostics.update { it.copy(isScanning = true, message = null) }
                delay(900)
                _demoDiagnostics.value = DiagnosticsState(
                    milOn = false, reportedCount = 0, lastScanMs = System.currentTimeMillis(),
                    message = "Códigos apagados (demonstração).", isDemo = true
                )
            }
        } else {
            obdManager.requestClear()
        }
    }

    fun disconnectDevice() {
        _connectingAddress.value = null
        _liveDataReceived.value = false
        obdManager.disconnect()
    }

    private fun startTelemetrySource() {
        if (_isDemoMode.value) {
            obdSimulator.start(viewModelScope, _selectedProfile.value) { incoming ->
                _telemetry.value = incoming
            }
        }
    }

    fun updateAccelerometer(x: Float) {
        // Accelerometer X axis on phone in portrait gives lateral G
        // 9.81 m/s^2 = 1.0 G
        if (!_isDemoMode.value) {
            val gForce = (-x / 9.81f).coerceIn(-1.5f, 1.5f)
            _telemetry.value = _telemetry.value.copy(lateralGForce = gForce)
        }
    }

    override fun onCleared() {
        super.onCleared()
        obdSimulator.stop()
        obdManager.disconnect()
    }
}

private fun demoDiagnosticsSample() = DiagnosticsState(
    milOn = true,
    reportedCount = 1,
    stored = listOf(dtcEntryOf("P0420", DtcKind.STORED)),
    pending = listOf(dtcEntryOf("P0171", DtcKind.PENDING)),
    lastScanMs = System.currentTimeMillis(),
    isDemo = true
)

private const val PREFS_NAME = "cibaauto_prefs"
private const val KEY_PROFILE_ID = "selected_profile_id"
private const val KEY_LAST_DEVICE = "last_obd_device"
private const val KEY_GAUGE_SLOTS = "gauge_slots"
private const val KEY_CUSTOM_PROFILES = "custom_edited_profiles"

/** Telemetria zerada: usada ao abrir o app e ao sair do modo demonstração (nunca mostra valores falsos). */
private fun blankTelemetryData() = TelemetryData(
    speedKmh = 0f, avgSpeedKmh = 0f, rpm = 0f, fuelLevelPercent = 0f,
    batteryVoltage = 0f, coolantTempC = Float.NaN, oilTempC = Float.NaN,
    turboPressureKpa = 0f, engineLoadPercent = 0f, throttlePercent = 0f,
    instantConsumptionKmPerL = 0f, avgConsumptionKmPerL = 0f, autonomyKm = 0,
    elapsedTimeSeconds = 0L, isEngineRunning = false, isEvMode = false,
    ethanolPercent = Float.NaN, hybridBatterySocPercent = 0f,
    hybridSocAvailable = false, hybridDetailAvailable = false,
    hybridBatteryVoltageVolts = 0f, hybridBatteryCurrentAmps = 0f,
    hybridBatteryTempC = 0f, icePowerKw = 0f, electricMotorPowerKw = 0f,
    generatorPowerKw = 0f,
    ecuLinkOk = false, fuelLevelAvailable = false
)
