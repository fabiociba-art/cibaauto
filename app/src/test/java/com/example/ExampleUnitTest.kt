package com.example

import com.example.model.DriveMode
import com.example.model.HybridEnergyFlowState
import com.example.model.VehicleProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVehicleProfilesIncludeCorollaHybridAndEthanolSupport() {
        val hybridProfile = VehicleProfiles.DEFAULT_PROFILES.firstOrNull { it.id == "corolla_hybrid_2025" }
        assertNotNull(hybridProfile)
        assertTrue(hybridProfile!!.isHybrid)
        assertEquals(43.0, hybridProfile.tankCapacityLiters, 0.1)
        assertEquals(18.5, hybridProfile.inmetroUrbanKmPerL, 0.01)
    }

    @Test
    fun testEthanolPercentageCalculationFromPid0152() {
        // PID 01 52: Formula is A * 100 / 255.
        // For 100% Ethanol (E100), byte A = 255 -> 100%
        val fullEthanolByte = 255
        val fullPercent = (fullEthanolByte * 100f / 255f).toInt()
        assertEquals(100, fullPercent)

        // For E27 gasoline (Brazilian standard ~27% ethanol), byte A = 69 -> 27%
        val e27Byte = 69
        val e27Percent = (e27Byte * 100f / 255f).toInt()
        assertEquals(27, e27Percent)
    }

    @Test
    fun testAutonomyCalculation() {
        val profile = VehicleProfiles.DEFAULT_PROFILES.first { it.id == "corolla_hybrid_2025" }
        val tankPercent = 50f // half tank = 21.5L
        val litersRemaining = (tankPercent / 100.0) * profile.tankCapacityLiters
        val autonomy = (litersRemaining * profile.inmetroAvgKmPerL).toInt()
        assertTrue(autonomy > 300)
    }

    @Test
    fun testHybridEnergyFlowStates() {
        val evState = HybridEnergyFlowState.EV_DRIVE
        assertEquals("Modo Elétrico (Bateria -> MG2 -> Rodas)", evState.description)

        val regenState = HybridEnergyFlowState.REGEN_BRAKING
        assertEquals("Regeneração (Rodas -> MG2 -> Bateria)", regenState.description)
    }

    @Test
    fun testDriveModes() {
        assertEquals("ECO", DriveMode.ECO.label)
        assertEquals("NORMAL", DriveMode.NORMAL.label)
        assertEquals("SPORT", DriveMode.SPORT.label)
    }
}
