package com.example

import com.example.model.TelemetryData
import com.example.model.VehicleProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryReliabilityTest {

    @Test
    fun semLeituraDeCombustivel_mostraTracos() {
        val t = TelemetryData(fuelLevelAvailable = false, fuelLiters = Float.NaN)
        assertEquals("-- L", t.fuelLitersText)
        assertEquals("--%", t.fuelPercentText)
        assertEquals("-- km", t.autonomyText)
    }

    @Test
    fun autonomiaEstimadaTemPrefixoDeTil() {
        val t = TelemetryData(
            fuelLevelAvailable = true, fuelLevelPercent = 50f, fuelLiters = 21.5f,
            autonomyKm = 300, autonomyIsEstimated = true
        )
        assertEquals("~300 km", t.autonomyText)
        assertTrue(t.fuelLitersText.startsWith("21"))
        assertTrue(t.fuelLitersText.endsWith(" L"))
    }

    @Test
    fun autonomiaMedidaNaoTemPrefixo() {
        val t = TelemetryData(
            fuelLevelAvailable = true, autonomyKm = 412, autonomyIsEstimated = false
        )
        assertEquals("412 km", t.autonomyText)
    }

    @Test
    fun perfilPadraoECorollaGliHybrid2026ComTanqueDe43Litros() {
        val p = VehicleProfiles.DEFAULT
        assertEquals("corolla_gli_hybrid_2026", p.id)
        assertEquals(43.0, p.tankCapacityLiters, 0.001)
        // 50% de 43 L = 21,5 L
        assertEquals(21.5, p.tankCapacityLiters * 0.5, 0.001)
    }
}
