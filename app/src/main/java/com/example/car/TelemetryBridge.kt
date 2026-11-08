package com.example.car

import com.example.model.*
import com.example.obd.ObdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte bidirecional completa e reativa entre o app do smartphone (ViewModel)
 * e o subsistema Android Auto.
 * Compartilha todas as métricas, diagnósticos, viagens, perfis de veículos e ações do motorista.
 */
object TelemetryBridge {
    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _vehicleName = MutableStateFlow("CIBA AUTO")
    val vehicleName: StateFlow<String> = _vehicleName.asStateFlow()

    private val _isHybridPowerInverted = MutableStateFlow(false)
    val isHybridPowerInverted: StateFlow<Boolean> = _isHybridPowerInverted.asStateFlow()

    private val _alerts = MutableStateFlow<List<CarAlert>>(emptyList())
    val alerts: StateFlow<List<CarAlert>> = _alerts.asStateFlow()

    private val _diagnosticCodes = MutableStateFlow<List<DiagnosticCode>>(emptyList())
    val diagnosticCodes: StateFlow<List<DiagnosticCode>> = _diagnosticCodes.asStateFlow()

    private val _readinessMonitors = MutableStateFlow<Map<String, String>>(emptyMap())
    val readinessMonitors: StateFlow<Map<String, String>> = _readinessMonitors.asStateFlow()

    private val _vehicles = MutableStateFlow<List<VehicleProfile>>(PreloadedVehicles.all)
    val vehicles: StateFlow<List<VehicleProfile>> = _vehicles.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<VehicleProfile>(PreloadedVehicles.corollaGliHybrid2026)
    val selectedVehicle: StateFlow<VehicleProfile> = _selectedVehicle.asStateFlow()

    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    private val _ethanolHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val ethanolHistory: StateFlow<List<Pair<String, Int>>> = _ethanolHistory.asStateFlow()

    private val _btDevices = MutableStateFlow<List<ObdManager.BtDeviceInfo>>(emptyList())
    val btDevices: StateFlow<List<ObdManager.BtDeviceInfo>> = _btDevices.asStateFlow()

    private val _selectedBtAddress = MutableStateFlow<String?>(null)
    val selectedBtAddress: StateFlow<String?> = _selectedBtAddress.asStateFlow()

    private val _vehicleInfo = MutableStateFlow<ObdManager.VehicleInfo?>(null)
    val vehicleInfo: StateFlow<ObdManager.VehicleInfo?> = _vehicleInfo.asStateFlow()

    private val _vehicleInfoLoading = MutableStateFlow(false)
    val vehicleInfoLoading: StateFlow<Boolean> = _vehicleInfoLoading.asStateFlow()

    private val _hybridScanReport = MutableStateFlow<List<String>>(emptyList())
    val hybridScanReport: StateFlow<List<String>> = _hybridScanReport.asStateFlow()

    private val _hybridScanRunning = MutableStateFlow(false)
    val hybridScanRunning: StateFlow<Boolean> = _hybridScanRunning.asStateFlow()

    // Callbacks de controle do Android Auto -> Celular
    private var onConnectCallback: (() -> Unit)? = null
    private var onDisconnectCallback: (() -> Unit)? = null
    private var onToggleDemoCallback: (() -> Unit)? = null
    private var onClearDtcCallback: (() -> Unit)? = null
    private var onRefreshDiagnosticsCallback: (() -> Unit)? = null
    private var onResetTripCallback: (() -> Unit)? = null
    private var onSelectVehicleCallback: ((VehicleProfile) -> Unit)? = null
    private var onTogglePowerInvertCallback: (() -> Unit)? = null
    private var onSelectBtDeviceCallback: ((String?) -> Unit)? = null
    private var onRefreshBtDevicesCallback: (() -> Unit)? = null
    private var onRunHybridScanCallback: (() -> Unit)? = null
    private var onReadVehicleInfoCallback: (() -> Unit)? = null

    // Atualizações de estado vindas do ViewModel
    fun update(state: TelemetryState) {
        _telemetry.value = state
    }

    fun updateVehicleName(name: String) {
        _vehicleName.value = name
    }

    fun updatePowerInverted(inverted: Boolean) {
        _isHybridPowerInverted.value = inverted
    }

    fun updateAlerts(alerts: List<CarAlert>) {
        _alerts.value = alerts
    }

    fun updateDiagnostics(codes: List<DiagnosticCode>, readiness: Map<String, String>) {
        _diagnosticCodes.value = codes
        _readinessMonitors.value = readiness
    }

    fun updateVehicles(list: List<VehicleProfile>, selected: VehicleProfile) {
        _vehicles.value = list
        _selectedVehicle.value = selected
        _vehicleName.value = selected.fullName
    }

    fun updateTrips(trips: List<TripRecord>) {
        _trips.value = trips
    }

    fun updateEthanolHistory(history: List<Pair<String, Int>>) {
        _ethanolHistory.value = history
    }

    fun updateBluetooth(devices: List<ObdManager.BtDeviceInfo>, selectedAddress: String?) {
        _btDevices.value = devices
        _selectedBtAddress.value = selectedAddress
    }

    fun updateVehicleInfo(info: ObdManager.VehicleInfo?, loading: Boolean) {
        _vehicleInfo.value = info
        _vehicleInfoLoading.value = loading
    }

    fun updateHybridScan(report: List<String>, running: Boolean) {
        _hybridScanReport.value = report
        _hybridScanRunning.value = running
    }

    fun registerCallbacks(
        onConnect: () -> Unit,
        onDisconnect: () -> Unit,
        onToggleDemo: () -> Unit,
        onClearDtc: () -> Unit = {},
        onRefreshDiagnostics: () -> Unit = {},
        onResetTrip: () -> Unit = {},
        onSelectVehicle: (VehicleProfile) -> Unit = {},
        onTogglePowerInvert: () -> Unit = {},
        onSelectBtDevice: (String?) -> Unit = {},
        onRefreshBtDevices: () -> Unit = {},
        onRunHybridScan: () -> Unit = {},
        onReadVehicleInfo: () -> Unit = {}
    ) {
        onConnectCallback = onConnect
        onDisconnectCallback = onDisconnect
        onToggleDemoCallback = onToggleDemo
        onClearDtcCallback = onClearDtc
        onRefreshDiagnosticsCallback = onRefreshDiagnostics
        onResetTripCallback = onResetTrip
        onSelectVehicleCallback = onSelectVehicle
        onTogglePowerInvertCallback = onTogglePowerInvert
        onSelectBtDeviceCallback = onSelectBtDevice
        onRefreshBtDevicesCallback = onRefreshBtDevices
        onRunHybridScanCallback = onRunHybridScan
        onReadVehicleInfoCallback = onReadVehicleInfo
    }

    // Métodos invocados pela interface do carro
    fun requestConnect() { onConnectCallback?.invoke() }
    fun requestDisconnect() { onDisconnectCallback?.invoke() }
    fun requestToggleDemo() { onToggleDemoCallback?.invoke() }
    fun requestClearDtcCodes() { onClearDtcCallback?.invoke() }
    fun requestRefreshDiagnostics() { onRefreshDiagnosticsCallback?.invoke() }
    fun requestResetTrip() { onResetTripCallback?.invoke() }
    fun requestSelectVehicle(v: VehicleProfile) { onSelectVehicleCallback?.invoke(v) }
    fun requestTogglePowerInvert() { onTogglePowerInvertCallback?.invoke() }
    fun requestSelectBtDevice(addr: String?) { onSelectBtDeviceCallback?.invoke(addr) }
    fun requestRefreshBtDevices() { onRefreshBtDevicesCallback?.invoke() }
    fun requestRunHybridScan() { onRunHybridScanCallback?.invoke() }
    fun requestReadVehicleInfo() { onReadVehicleInfoCallback?.invoke() }
}
