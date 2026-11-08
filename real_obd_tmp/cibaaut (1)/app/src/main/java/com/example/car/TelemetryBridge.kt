package com.example.car

import com.example.model.TelemetryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte compartilhada entre o app do telefone e a sessão Android Auto.
 * O ViewModel do telefone atualiza este bridge; a tela do carro observa os valores.
 */
object TelemetryBridge {
    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _vehicleName = MutableStateFlow("CIBA AUTO")
    val vehicleName: StateFlow<String> = _vehicleName.asStateFlow()

    fun update(state: TelemetryState) {
        _telemetry.value = state
    }

    fun updateVehicleName(name: String) {
        _vehicleName.value = name
    }
}
