package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow

enum class HybridEnergyFlowState(val description: String, val color: Color) {
    EV_DRIVE("Modo Elétrico (Bateria -> MG2 -> Rodas)", GaugeGreen),
    HYBRID_DRIVE("Modo Híbrido (Combustão + Elétrico)", GaugeYellow),
    REGEN_BRAKING("Regeneração (Rodas -> MG2 -> Bateria)", GaugeCyan),
    ENGINE_CHARGING("Carga Parada (Combustão -> MG1 -> Bateria)", GaugeYellow),
    IDLE("Sistema Híbrido Pronto (Pronto)", GaugeGreen)
}

data class TelemetryData(
    val speedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val rpm: Float = 1100f,
    val fuelLevelPercent: Float = 85f,
    val batteryVoltage: Float = 13.8f,
    val coolantTempC: Float = 90f,
    val oilTempC: Float = 88f,
    val turboPressureKpa: Float = 12f,
    val engineLoadPercent: Float = 22f,
    val throttlePercent: Float = 18f,
    val lateralGForce: Float = 0.0f,
    val instantConsumptionKmPerL: Float = 16.8f,
    val avgConsumptionKmPerL: Float = 16.2f,
    // true = valor estimado pela tabela INMETRO (ECU não informa vazão de combustível)
    val consumptionIsEstimated: Boolean = false,
    val autonomyKm: Int = 362,
    val driveMode: DriveMode = DriveMode.ECO,
    val elapsedTimeSeconds: Long = 720L,
    val isEngineRunning: Boolean = true,
    val isEvMode: Boolean = true,

    // Ethanol Percentage from ECU (PID 0152)
    val ethanolPercent: Float = 27f,

    // Hybrid Synergy Flow Telemetry (PID / ECU)
    val hybridBatterySocPercent: Float = 72f,
    // false = SOC não disponível via OBD neste veículo (mostrar "--")
    val hybridSocAvailable: Boolean = true,
    // false = tensão/corrente/temperatura/potências da bateria HV não disponíveis via OBD (mostrar "--")
    val hybridDetailAvailable: Boolean = true,
    val hybridBatteryVoltageVolts: Float = 207.4f,
    val hybridBatteryCurrentAmps: Float = 14.2f,
    val hybridBatteryTempC: Float = 31f,
    // true = a temperatura da bateria HV foi realmente lida (sem leitura real, sem mostrador)
    val hybridBatteryTempAvailable: Boolean = false,
    val icePowerKw: Float = 38f,
    val electricMotorPowerKw: Float = 24f,
    val generatorPowerKw: Float = 12f,
    val hybridFlowState: HybridEnergyFlowState = HybridEnergyFlowState.IDLE,

    // ---- Confiabilidade dos dados (Fase 1) ----
    // false = a ECU parou de responder; valores de sensores não são atuais
    val ecuLinkOk: Boolean = true,
    // false = nível de combustível nunca lido ou obsoleto (mostrar "--")
    val fuelLevelAvailable: Boolean = true,
    // Litros restantes = capacidade do tanque do perfil x percentual lido (valor CALCULADO)
    val fuelLiters: Float = Float.NaN,
    // true = autonomia usa a média INMETRO do perfil (ainda sem média real suficiente na viagem)
    val autonomyIsEstimated: Boolean = false,

    // ---- Sensores adicionais (Fase 2): id do SensorRegistry -> valor ATUAL.
    // Sensor ausente do mapa = não suportado, ainda não lido ou obsoleto (mostrar "--").
    val sensors: Map<String, Float> = emptyMap()
) {
    val fuelLitersText: String
        get() = if (fuelLevelAvailable && !fuelLiters.isNaN()) "%.1f L".format(fuelLiters) else "-- L"

    val fuelPercentText: String
        get() = if (fuelLevelAvailable) "${fuelLevelPercent.toInt()}%" else "--%"

    /** Ex.: "21,5 L · 50%". */
    val fuelSummaryText: String
        get() = "$fuelLitersText · $fuelPercentText"

    /** "~" = estimada com a média INMETRO; "--" = sem leitura do tanque. */
    val autonomyText: String
        get() = if (!fuelLevelAvailable) "-- km"
        else (if (autonomyIsEstimated) "~" else "") + "$autonomyKm km"

    val speedColor: Color
        get() = when {
            speedKmh <= 50f -> GaugeGreen
            speedKmh <= 90f -> GaugeYellow
            else -> GaugeRed
        }

    val rpmColor: Color
        get() = when {
            rpm <= 1500f -> GaugeGreen
            rpm <= 3000f -> GaugeYellow
            else -> GaugeRed
        }

    val batteryColor: Color
        get() = when {
            batteryVoltage in 12.4f..14.2f -> GaugeGreen
            batteryVoltage in 11.9f..12.39f -> GaugeYellow
            else -> GaugeRed
        }

    val fuelColor: Color
        get() = when {
            fuelLevelPercent >= 70f -> GaugeGreen
            fuelLevelPercent >= 45f -> GaugeYellow
            else -> GaugeRed
        }

    val coolantColor: Color
        get() = when {
            coolantTempC in 80f..100f -> GaugeGreen
            coolantTempC in 60f..79.9f || coolantTempC in 100.1f..110f -> GaugeYellow
            else -> GaugeRed
        }

    val oilTempColor: Color
        get() = when {
            oilTempC in 75f..105f -> GaugeGreen
            oilTempC in 50f..74.9f || oilTempC in 105.1f..120f -> GaugeYellow
            else -> GaugeRed
        }

    val engineLoadColor: Color
        get() = when {
            engineLoadPercent <= 30f -> GaugeGreen
            engineLoadPercent <= 70f -> GaugeYellow
            else -> GaugeRed
        }

    val throttleColor: Color
        get() = when {
            throttlePercent <= 25f -> GaugeGreen
            throttlePercent <= 60f -> GaugeYellow
            else -> GaugeRed
        }

    val ethanolColor: Color
        get() = when {
            ethanolPercent <= 30f -> GaugeCyan   // Gasolina E27 padrão
            ethanolPercent <= 70f -> GaugeYellow // Mistura intermediária
            else -> GaugeGreen                  // Etanol hidratado alto / E100
        }

    fun consumptionColor(inmetroRef: Double): Color {
        val ref = inmetroRef.toFloat()
        return when {
            instantConsumptionKmPerL >= ref -> GaugeGreen
            instantConsumptionKmPerL >= ref * 0.75f -> GaugeYellow
            else -> GaugeRed
        }
    }

    val formattedElapsedTime: String
        get() {
            val hours = elapsedTimeSeconds / 3600
            val minutes = (elapsedTimeSeconds % 3600) / 60
            return "%02d:%02dh".format(hours, minutes)
        }
}
