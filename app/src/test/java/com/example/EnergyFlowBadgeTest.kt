package com.example

import com.example.model.HybridEnergyFlowState
import com.example.model.TelemetryData
import com.example.ui.components.EnergyFlowBadge
import com.example.ui.components.classifyEnergyFlow
import com.example.ui.components.deriveEnergyFlowVisual
import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyFlowBadgeTest {

    private fun badge(t: TelemetryData) = classifyEnergyFlow(deriveEnergyFlowVisual(t))

    private fun detail(amps: Float, rpm: Float, speed: Float) = TelemetryData(
        ecuLinkOk = true, hybridDetailAvailable = true, hybridBatteryCurrentAmps = amps,
        rpm = rpm, speedKmh = speed, isEngineRunning = rpm > 300f
    )

    @Test
    fun semLinkComAEcuNaoHaSelo() {
        val t = TelemetryData(ecuLinkOk = false, hybridDetailAvailable = true, hybridBatteryCurrentAmps = 20f, speedKmh = 50f)
        assertEquals(EnergyFlowBadge.NONE, badge(t))
    }

    @Test
    fun comCorrenteDaBateria() {
        assertEquals(EnergyFlowBadge.EV, badge(detail(amps = 10f, rpm = 0f, speed = 30f)))
        assertEquals(EnergyFlowBadge.CHARGING, badge(detail(amps = -10f, rpm = 0f, speed = 30f)))
        assertEquals(EnergyFlowBadge.EV_PLUS_COMBUSTION, badge(detail(amps = 10f, rpm = 1500f, speed = 30f)))
        assertEquals(EnergyFlowBadge.COMBUSTION, badge(detail(amps = -10f, rpm = 1500f, speed = 0f)))
        assertEquals(EnergyFlowBadge.NONE, badge(detail(amps = 0f, rpm = 0f, speed = 0f)))
    }

    @Test
    fun semCorrenteUsaSomenteOEstadoHibrido() {
        fun state(s: HybridEnergyFlowState, throttle: Float = 0f) = TelemetryData(
            ecuLinkOk = true, hybridDetailAvailable = false, hybridFlowState = s, throttlePercent = throttle
        )
        assertEquals(EnergyFlowBadge.CHARGING, badge(state(HybridEnergyFlowState.REGEN_BRAKING)))
        assertEquals(EnergyFlowBadge.EV, badge(state(HybridEnergyFlowState.EV_DRIVE)))
        assertEquals(EnergyFlowBadge.EV_PLUS_COMBUSTION, badge(state(HybridEnergyFlowState.HYBRID_DRIVE, 70f)))
        assertEquals(EnergyFlowBadge.COMBUSTION, badge(state(HybridEnergyFlowState.HYBRID_DRIVE, 20f)))
        assertEquals(EnergyFlowBadge.COMBUSTION, badge(state(HybridEnergyFlowState.ENGINE_CHARGING)))
        assertEquals(EnergyFlowBadge.NONE, badge(state(HybridEnergyFlowState.IDLE)))
    }
}
