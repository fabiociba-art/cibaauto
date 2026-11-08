package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDeriveEnergyMode_standby() {
    val t = com.example.model.TelemetryState(speed = 0f, rpm = 0, hybridMotorPowerKw = 0f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true)
    assertEquals(com.example.model.EnergyMode.STANDBY, mode)
  }

  @Test
  fun testDeriveEnergyMode_ev() {
    val t = com.example.model.TelemetryState(speed = 25f, rpm = 0, hybridMotorPowerKw = 12f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true)
    assertEquals(com.example.model.EnergyMode.EV, mode)
  }

  @Test
  fun testDeriveEnergyMode_charging() {
    // Regenerative braking with negative power
    val t = com.example.model.TelemetryState(speed = 40f, rpm = 0, hybridMotorPowerKw = -15f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true)
    assertEquals(com.example.model.EnergyMode.CHARGING, mode)
  }

  @Test
  fun testDeriveEnergyMode_charging_with_inverted_sign() {
    // When invertKwSign is true, positive raw value becomes negative effective power (charging)
    val t = com.example.model.TelemetryState(speed = 40f, rpm = 0, hybridMotorPowerKw = 15f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true, invertKwSign = true)
    assertEquals(com.example.model.EnergyMode.CHARGING, mode)
  }

  @Test
  fun testDeriveEnergyMode_engine() {
    // Combustion engine running with near zero electric assist
    val t = com.example.model.TelemetryState(speed = 85f, rpm = 1800, hybridMotorPowerKw = 0.4f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true)
    assertEquals(com.example.model.EnergyMode.ENGINE, mode)
  }

  @Test
  fun testDeriveEnergyMode_hybrid() {
    // Combustion engine + strong electric assist
    val t = com.example.model.TelemetryState(speed = 90f, rpm = 3200, hybridMotorPowerKw = 25f)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = true)
    assertEquals(com.example.model.EnergyMode.HYBRID, mode)
  }

  @Test
  fun testDeriveEnergyMode_nonHybridVehicle() {
    val t = com.example.model.TelemetryState(speed = 60f, rpm = 2200)
    val mode = com.example.model.deriveEnergyMode(t, isHybridVehicle = false)
    assertEquals(com.example.model.EnergyMode.ENGINE, mode)

    val stopped = com.example.model.TelemetryState(speed = 0f, rpm = 800)
    val stoppedMode = com.example.model.deriveEnergyMode(stopped, isHybridVehicle = false)
    assertEquals(com.example.model.EnergyMode.STANDBY, stoppedMode)
  }
}
