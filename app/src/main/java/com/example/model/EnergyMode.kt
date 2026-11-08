package com.example.model

import kotlin.math.abs

// ============================================================================
// CONSTANTES DE CALIBRAÇÃO DO MODO DE ENERGIA (LIMIARES DE TRANSIÇÃO)
// ============================================================================
const val SPEED_STANDBY_THRESHOLD_KMH = 1.2f
const val POWER_STANDBY_THRESHOLD_KW = 0.8f
const val POWER_HYBRID_THRESHOLD_KW = 1.8f
const val RPM_ENGINE_ON_THRESHOLD = 200

// ============================================================================
// ENUM DE MODOS DE ENERGIA (COMPARTILHADO ENTRE CELULAR E ANDROID AUTO)
// ============================================================================
enum class EnergyMode(
    val displayName: String,
    val subtitle: String,
    val colorArgb: Long
) {
    STANDBY("STANDBY", "Pronto / Em Espera", 0xFF64748BL),
    EV("EV", "100% Elétrico (Bateria → Rodas)", 0xFF00F0FFL),
    ENGINE("COMBUSTÃO", "Motor Térmico em Tração", 0xFFFF9100L),
    HYBRID("HÍBRIDO", "Combustão + Elétrico Combinados", 0xFF00FFCCL),
    CHARGING("CARREGANDO", "Regeneração de Energia (→ Bateria)", 0xFF00FF66L)
}

/**
 * Função pura e testável que deriva o modo de operação do trem de força híbrido
 * a partir do estado de telemetria OBD-2.
 * Compartilhada entre o aplicativo móvel e o Android Auto.
 */
fun deriveEnergyMode(
    telemetry: TelemetryState,
    isHybridVehicle: Boolean = true,
    invertKwSign: Boolean = false
): EnergyMode {
    if (!isHybridVehicle) {
        return if (telemetry.rpm > RPM_ENGINE_ON_THRESHOLD && telemetry.speed > SPEED_STANDBY_THRESHOLD_KMH) {
            EnergyMode.ENGINE
        } else {
            EnergyMode.STANDBY
        }
    }

    val rawKw = telemetry.hybridMotorPowerKw
    val effectiveKw = if (invertKwSign) -rawKw else rawKw
    val rpm = telemetry.rpm
    val speed = telemetry.speed

    val isEngineOn = rpm > RPM_ENGINE_ON_THRESHOLD
    val isMoving = speed > SPEED_STANDBY_THRESHOLD_KMH

    // 1. CARREGANDO: Potência negativa (regeneração na frenagem ou combustão recarregando bateria)
    if (effectiveKw < -POWER_STANDBY_THRESHOLD_KW) {
        return EnergyMode.CHARGING
    }

    // 2. STANDBY: Quase parado e potência elétrica neutra
    if (!isMoving && abs(effectiveKw) <= POWER_STANDBY_THRESHOLD_KW && (!isEngineOn || rpm <= RPM_ENGINE_ON_THRESHOLD)) {
        return EnergyMode.STANDBY
    }

    // 3. EV: Motor a combustão desligado (RPM == 0) e o carro está em movimento ou descarregando bateria
    if (!isEngineOn) {
        return if (isMoving || effectiveKw >= POWER_STANDBY_THRESHOLD_KW) {
            EnergyMode.EV
        } else {
            EnergyMode.STANDBY
        }
    }

    // 4. Motor a combustão está ligado (RPM > 200)
    // Se houver assistência elétrica relevante -> HÍBRIDO
    // Caso contrário -> COMBUSTÃO PURA (Cruzeiro térmico)
    return if (effectiveKw >= POWER_HYBRID_THRESHOLD_KW) {
        EnergyMode.HYBRID
    } else {
        EnergyMode.ENGINE
    }
}

/**
 * Representa um alerta de segurança veicular para exibição no celular e no Android Auto.
 */
data class CarAlert(
    val id: String,
    val title: String,
    val message: String,
    val isCritical: Boolean
)

/**
 * Avaliador de alertas de telemetria automotiva (temperatura, tensão, rotação, bateria HV).
 */
object AlertEvaluator {
    fun evaluate(telemetry: TelemetryState): List<CarAlert> {
        val alerts = mutableListOf<CarAlert>()

        // 1. Temperatura do líquido de arrefecimento (Coolant)
        if (telemetry.coolantTemp >= 105) {
            alerts.add(
                CarAlert(
                    id = "high_coolant_critical",
                    title = "ALERTA: Superaquecimento do Motor",
                    message = "Líquido de arrefecimento em ${telemetry.coolantTemp}°C (Crítico > 105°C). Pare o veículo.",
                    isCritical = true
                )
            )
        } else if (telemetry.coolantTemp >= 98) {
            alerts.add(
                CarAlert(
                    id = "high_coolant_warn",
                    title = "Atenção: Temperatura Elevada",
                    message = "Temperatura da água em ${telemetry.coolantTemp}°C (Atenção > 98°C).",
                    isCritical = false
                )
            )
        }

        // 2. Tensão da Bateria 12V
        if (telemetry.batteryVoltage > 0f) {
            if (telemetry.batteryVoltage < 11.8f) {
                alerts.add(
                    CarAlert(
                        id = "low_battery_voltage",
                        title = "Atenção: Bateria 12V Baixa",
                        message = "Tensão em ${String.format("%.1f", telemetry.batteryVoltage)}V (Limite mín. 11.8V).",
                        isCritical = false
                    )
                )
            } else if (telemetry.batteryVoltage > 15.2f) {
                alerts.add(
                    CarAlert(
                        id = "high_battery_voltage",
                        title = "ALERTA: Sobretensão no Alternador",
                        message = "Tensão em ${String.format("%.1f", telemetry.batteryVoltage)}V (Acima de 15.2V).",
                        isCritical = true
                    )
                )
            }
        }

        // 3. Sobregiro de RPM
        if (telemetry.rpm >= 6000) {
            alerts.add(
                CarAlert(
                    id = "high_rpm",
                    title = "Atenção: Rotação Elevada",
                    message = "Motor em ${telemetry.rpm} RPM. Reduza a aceleração.",
                    isCritical = false
                )
            )
        }

        // 4. Bateria Híbrida de Alta Tensão (HV)
        if (telemetry.isHybridDetected && telemetry.hybridBatterySoc in 1..19) {
            alerts.add(
                CarAlert(
                    id = "low_hv_soc",
                    title = "Aviso: Bateria HV em Carga Baixa",
                    message = "Carga da bateria híbrida em ${telemetry.hybridBatterySoc}%.",
                    isCritical = false
                )
            )
        }

        return alerts
    }
}
