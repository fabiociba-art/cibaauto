package com.example.obd

import com.example.model.DriveMode
import com.example.model.HybridEnergyFlowState
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

class ObdSimulator {

    private var simJob: Job? = null
    private var tick = 0L

    fun start(
        scope: CoroutineScope,
        profile: VehicleProfile,
        onUpdate: (TelemetryData) -> Unit
    ) {
        stop()
        simJob = scope.launch(Dispatchers.Default) {
            var currentSpeed = 0f
            var currentRpm = 1100f
            var targetSpeed = 65f
            var fuel = 85f
            var avgSpeedAccum = 0.0
            var speedSamples = 0
            var elapsedSeconds = 720L // Initial 00:12h
            var distKm = 0.0
            var fuelL = 0.0

            try {
                while (isActive) {
                    tick++
                    delay(120) // ~8 Hz smooth updates

                    if (tick % 8 == 0L) {
                        elapsedSeconds++
                    }

                    // Cycle driving scenarios: city acceleration, cruising, highway spurt, braking
                    val cyclePhase = ((tick / 25) % 6).toInt()
                    when (cyclePhase) {
                        0 -> { // City cruising
                            targetSpeed = 48f
                        }
                        1 -> { // Acceleration burst
                            targetSpeed = 82f
                        }
                        2 -> { // Highway speed
                            targetSpeed = 115f
                        }
                        3 -> { // High speed overtakes
                            targetSpeed = 128f
                        }
                        4 -> { // Deceleration / braking
                            targetSpeed = 35f
                        }
                        5 -> { // Stop / slow traffic
                            targetSpeed = 0f
                        }
                    }

                    // Smooth speed lerp
                    val speedDiff = targetSpeed - currentSpeed
                    val speedStep = if (speedDiff > 0) 1.8f else 2.6f
                    currentSpeed += (speedDiff * 0.06f).coerceIn(-speedStep, speedStep)
                    if (currentSpeed < 0f) currentSpeed = 0f

                    // RPM calculation based on simulated CVT / multi-gear ratio
                    val baseGearRatio = when {
                        currentSpeed < 10f -> 120f
                        currentSpeed < 40f -> 55f
                        currentSpeed < 70f -> 36f
                        currentSpeed < 100f -> 26f
                        else -> 22f
                    }

                    val accelFactor = (targetSpeed - currentSpeed).coerceAtLeast(0f)
                    var targetRpm = if (currentSpeed < 1f) {
                        if (profile.isHybrid && tick % 150 < 60) 0f else 1100f
                    } else {
                        (currentSpeed * baseGearRatio + accelFactor * 45f).coerceIn(800f, 5500f)
                    }

                    // Add subtle mechanical jitter
                    targetRpm += (sin(tick * 0.3) * 35.0).toFloat()
                    currentRpm += (targetRpm - currentRpm) * 0.15f
                    if (currentRpm < 0f) currentRpm = 0f

                    // Coolant and Oil temperature stabilization
                    val coolant = 90f + (sin(tick * 0.05) * 1.5).toFloat()
                    val oil = 88f + (sin(tick * 0.04) * 2.0).toFloat()

                    // Engine Load and Throttle
                    val throttle = if (speedDiff > 2f) {
                        (40f + speedDiff * 2.5f).coerceIn(10f, 95f)
                    } else if (currentSpeed > 5f) {
                        22f + (sin(tick * 0.1) * 5.0).toFloat()
                    } else {
                        0f
                    }

                    val engineLoad = (throttle * 0.85f + (currentRpm / 6000f) * 25f).coerceIn(8f, 98f)

                    // Turbo pressure (boosts on acceleration)
                    val turbo = if (profile.hasTurbo) {
                        (accelFactor * 3.5f + (currentRpm / 100f) * 0.8f).coerceIn(0f, 92f)
                    } else {
                        (engineLoad * 0.45f).coerceIn(0f, 45f)
                    }

                    // Battery voltage
                    val battery = if (currentRpm > 600) {
                        13.8f + (sin(tick * 0.15) * 0.3).toFloat()
                    } else {
                        12.5f + (sin(tick * 0.15) * 0.2).toFloat()
                    }

                    // Lateral G-Force (simulates gentle steering oscillations and turns)
                    val lateralG = if (currentSpeed > 20f) {
                        ((sin(tick * 0.08) * 0.35) * (currentSpeed / 80f)).toFloat().coerceIn(-0.8f, 0.8f)
                    } else {
                        0f
                    }

                    // Fuel depletion very slow
                    fuel -= 0.0005f
                    if (fuel < 5f) fuel = 85f

                    // Average speed
                    if (currentSpeed > 0) {
                        avgSpeedAccum += currentSpeed
                        speedSamples++
                    }
                    val avgSpeed = if (speedSamples > 0) (avgSpeedAccum / speedSamples).toFloat() else 0f

                    // Drive mode evaluation
                    val driveMode = when {
                        profile.isHybrid && currentRpm < 400 && currentSpeed < 45 -> DriveMode.ECO
                        engineLoad > 68f || currentRpm > 3200f || currentSpeed > 95f -> DriveMode.SPORT
                        engineLoad < 35f && currentSpeed < 60f -> DriveMode.ECO
                        else -> DriveMode.NORMAL
                    }

                    // Consumption calculation
                    val baseCons = when (driveMode) {
                        DriveMode.ECO -> profile.inmetroUrbanKmPerL
                        DriveMode.NORMAL -> profile.inmetroAvgKmPerL
                        DriveMode.SPORT -> profile.inmetroRoadKmPerL * 0.75
                    }
                    val instantCons = (baseCons + (sin(tick * 0.2) * 1.8)).toFloat().coerceAtLeast(3.0f)
                    if (currentSpeed > 1f) {
                        val dtH = 0.12 / 3600.0
                        distKm += currentSpeed * dtH
                        fuelL += currentSpeed * dtH / instantCons
                    }
                    val avgCons = if (fuelL > 0.001) (distKm / fuelL).toFloat() else profile.inmetroAvgKmPerL.toFloat()

                    // Autonomy in Km
                    val remainingLitres = profile.tankCapacityLiters * (fuel / 100.0)
                    val autonomy = (remainingLitres * profile.inmetroAvgKmPerL).toInt()

                    // Hybrid Flow State Simulation
                    val hybridFlow = when {
                        speedDiff < -2f && currentSpeed > 5f -> HybridEnergyFlowState.REGEN_BRAKING
                        profile.isHybrid && currentRpm < 300 && currentSpeed > 0 -> HybridEnergyFlowState.EV_DRIVE
                        currentRpm > 300 && currentSpeed > 0 -> HybridEnergyFlowState.HYBRID_DRIVE
                        currentSpeed == 0f && currentRpm > 300 -> HybridEnergyFlowState.ENGINE_CHARGING
                        else -> HybridEnergyFlowState.IDLE
                    }

                    // Hybrid battery SOC % (60% to 80%)
                    val hvSoc = 72f + (sin(tick * 0.03) * 6.0).toFloat()
                    val hvVoltage = 207.2f + (sin(tick * 0.04) * 3.5).toFloat()
                    val hvCurrent = when (hybridFlow) {
                        HybridEnergyFlowState.EV_DRIVE -> 24.5f
                        HybridEnergyFlowState.HYBRID_DRIVE -> 12.0f
                        HybridEnergyFlowState.REGEN_BRAKING -> -28.0f // Charging
                        HybridEnergyFlowState.ENGINE_CHARGING -> -15.0f // Charging
                        HybridEnergyFlowState.IDLE -> 0.0f
                    }

                    val iceKw = if (currentRpm > 300) (engineLoad * 0.9f).coerceIn(15f, 72f) else 0f
                    val evKw = if (hybridFlow == HybridEnergyFlowState.EV_DRIVE || hybridFlow == HybridEnergyFlowState.HYBRID_DRIVE) {
                        (throttle * 0.5f).coerceIn(5f, 53f)
                    } else 0f

                    val data = TelemetryData(
                        speedKmh = currentSpeed,
                        avgSpeedKmh = avgSpeed,
                        rpm = currentRpm,
                        fuelLevelPercent = fuel,
                        fuelLiters = (profile.tankCapacityLiters * fuel / 100.0).toFloat(),
                        batteryVoltage = battery,
                        coolantTempC = coolant,
                        oilTempC = oil,
                        turboPressureKpa = turbo,
                        engineLoadPercent = engineLoad,
                        throttlePercent = throttle,
                        lateralGForce = lateralG,
                        instantConsumptionKmPerL = instantCons,
                        avgConsumptionKmPerL = avgCons,
                        autonomyKm = autonomy,
                        driveMode = driveMode,
                        elapsedTimeSeconds = elapsedSeconds,
                        isEngineRunning = currentRpm > 300,
                        isEvMode = profile.isHybrid && currentRpm < 300,
                        ethanolPercent = if (profile.fuelType.contains("flex", ignoreCase = true)) 27.5f else Float.NaN,
                        hybridBatterySocPercent = hvSoc,
                        hybridBatteryVoltageVolts = hvVoltage,
                        hybridBatteryCurrentAmps = hvCurrent,
                        hybridBatteryTempC = 31.5f,
                        hybridBatteryTempAvailable = true,
                        icePowerKw = iceKw,
                        electricMotorPowerKw = evKw,
                        generatorPowerKw = if (currentRpm > 300) 14.5f else 0f,
                        hybridFlowState = hybridFlow
                    )

                    withContext(Dispatchers.Main) {
                        onUpdate(data)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun stop() {
        simJob?.cancel()
        simJob = null
    }
}
