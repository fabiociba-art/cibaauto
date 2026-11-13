package com.example

import com.example.model.HybridEnergyFlowState
import com.example.model.TelemetryData
import com.example.ui.components.EnergyFlowVisual
import com.example.ui.components.deriveEnergyFlowVisual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyFlowVisualTest {

    @Test
    fun semLinkEcu_retornaTudoFalso() {
        val t = TelemetryData(
            ecuLinkOk = false,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = 15f,
            speedKmh = 50f,
            isEngineRunning = true,
            rpm = 1500f
        )
        val visual = deriveEnergyFlowVisual(t)
        assertFalse(visual.hasAnyFlow)
        assertFalse(visual.engineToWheels)
        assertFalse(visual.batteryToWheels)
        assertFalse(visual.wheelsToBattery)
        assertFalse(visual.engineToBattery)
    }

    @Test
    fun estadoPadraoTelemetryDataEIdle_semFluxo() {
        val t = TelemetryData()
        assertEquals(HybridEnergyFlowState.IDLE, t.hybridFlowState)
    }

    @Test
    fun detalheDisponivel_regeneracaoFreio_apenasRodasParaBateria() {
        val t = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = -12f, // charging (< -1)
            speedKmh = 45f,                  // moving (> 1)
            isEngineRunning = false,
            rpm = 0f
        )
        val visual = deriveEnergyFlowVisual(t)
        assertTrue(visual.hasAnyFlow)
        assertTrue(visual.wheelsToBattery)
        assertFalse(visual.engineToWheels)
        assertFalse(visual.batteryToWheels)
        assertFalse(visual.engineToBattery)
    }

    @Test
    fun detalheDisponivel_modoEv_apenasBateriaParaRodas() {
        val t = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = 18f, // discharging (> 1)
            speedKmh = 35f,                 // moving (> 1)
            isEngineRunning = false,
            rpm = 0f
        )
        val visual = deriveEnergyFlowVisual(t)
        assertTrue(visual.hasAnyFlow)
        assertTrue(visual.batteryToWheels)
        assertFalse(visual.engineToWheels)
        assertFalse(visual.wheelsToBattery)
        assertFalse(visual.engineToBattery)
    }

    @Test
    fun detalheDisponivel_hibridoAmbos_motorEBateriaParaRodas() {
        val t = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = 25f, // discharging (> 1)
            speedKmh = 70f,                 // moving (> 1)
            isEngineRunning = true,
            rpm = 2200f                     // engineOn (> 400)
        )
        val visual = deriveEnergyFlowVisual(t)
        assertTrue(visual.hasAnyFlow)
        assertTrue(visual.engineToWheels)
        assertTrue(visual.batteryToWheels)
        assertFalse(visual.wheelsToBattery)
        assertFalse(visual.engineToBattery)
    }

    @Test
    fun detalheDisponivel_combustaoComCarga_motorParaRodasEBateria() {
        val t = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = -8f, // charging (< -1)
            speedKmh = 60f,                 // moving (> 1)
            isEngineRunning = true,
            rpm = 1800f                     // engineOn (> 400)
        )
        val visual = deriveEnergyFlowVisual(t)
        assertTrue(visual.hasAnyFlow)
        assertTrue(visual.engineToWheels)
        assertFalse(visual.batteryToWheels)
        assertFalse(visual.wheelsToBattery)
        assertTrue(visual.engineToBattery)
    }

    @Test
    fun detalheDisponivel_paradoCarregando_apenasMotorParaBateria() {
        val t = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = true,
            hybridBatteryCurrentAmps = -15f, // charging (< -1)
            speedKmh = 0f,                   // not moving (<= 1)
            isEngineRunning = true,
            rpm = 1200f
        )
        val visual = deriveEnergyFlowVisual(t)
        assertTrue(visual.hasAnyFlow)
        assertFalse(visual.engineToWheels)
        assertFalse(visual.batteryToWheels)
        assertFalse(visual.wheelsToBattery)
        assertTrue(visual.engineToBattery)
    }

    @Test
    fun fallbackPeloEstado_quandoSemDetalhes() {
        // REGEN_BRAKING
        val tRegen = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.REGEN_BRAKING
        )
        assertEquals(EnergyFlowVisual(wheelsToBattery = true), deriveEnergyFlowVisual(tRegen))

        // EV_DRIVE
        val tEv = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.EV_DRIVE
        )
        assertEquals(EnergyFlowVisual(batteryToWheels = true), deriveEnergyFlowVisual(tEv))

        // HYBRID_DRIVE pedal moderado (< 60%)
        val tHybridNormal = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.HYBRID_DRIVE,
            throttlePercent = 40f
        )
        assertEquals(EnergyFlowVisual(engineToWheels = true, batteryToWheels = false), deriveEnergyFlowVisual(tHybridNormal))

        // HYBRID_DRIVE pisando fundo (>= 60%)
        val tHybridPower = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.HYBRID_DRIVE,
            throttlePercent = 75f
        )
        assertEquals(EnergyFlowVisual(engineToWheels = true, batteryToWheels = true), deriveEnergyFlowVisual(tHybridPower))

        // ENGINE_CHARGING
        val tCharging = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.ENGINE_CHARGING
        )
        assertEquals(EnergyFlowVisual(engineToBattery = true), deriveEnergyFlowVisual(tCharging))

        // IDLE
        val tIdle = TelemetryData(
            ecuLinkOk = true,
            hybridDetailAvailable = false,
            hybridFlowState = HybridEnergyFlowState.IDLE
        )
        assertEquals(EnergyFlowVisual(), deriveEnergyFlowVisual(tIdle))
        assertFalse(deriveEnergyFlowVisual(tIdle).hasAnyFlow)
    }
}
