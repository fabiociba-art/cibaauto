package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HybridEnergyFlowState
import com.example.model.TelemetryData
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Estado visual puro do fluxo de energia híbrido do cluster Toyota Corolla.
 */
data class EnergyFlowVisual(
    val engineToWheels: Boolean = false,
    val batteryToWheels: Boolean = false,
    val wheelsToBattery: Boolean = false,
    val engineToBattery: Boolean = false
) {
    val hasAnyFlow: Boolean
        get() = engineToWheels || batteryToWheels || wheelsToBattery || engineToBattery
}

/**
 * Função pura que deriva o estado visual do fluxo estritamente a partir da telemetria da ECU:
 * - Se !t.ecuLinkOk: tudo false (sem fluxo)
 * - Se t.hybridDetailAvailable:
 *     charging = hybridBatteryCurrentAmps < -1
 *     discharging = hybridBatteryCurrentAmps > 1
 *     engineOn = isEngineRunning && rpm > 400
 *     moving = speedKmh > 1
 *     engineToWheels = engineOn && moving
 *     batteryToWheels = discharging && moving
 *     wheelsToBattery = charging && moving && !engineOn
 *     engineToBattery = charging && engineOn
 * - Senão, SOMENTE pelo hybridFlowState:
 *     REGEN_BRAKING -> wheelsToBattery
 *     EV_DRIVE -> batteryToWheels
 *     HYBRID_DRIVE -> engineToWheels (+ batteryToWheels se throttlePercent >= 60)
 *     ENGINE_CHARGING -> engineToBattery
 *     IDLE -> tudo false
 */
fun deriveEnergyFlowVisual(t: TelemetryData): EnergyFlowVisual {
    if (!t.ecuLinkOk) {
        return EnergyFlowVisual()
    }

    return if (t.hybridDetailAvailable) {
        val charging = t.hybridBatteryCurrentAmps < -1f
        val discharging = t.hybridBatteryCurrentAmps > 1f
        val engineOn = t.isEngineRunning && t.rpm > 400f
        val moving = t.speedKmh > 1f

        EnergyFlowVisual(
            engineToWheels = engineOn && moving,
            batteryToWheels = discharging && moving,
            wheelsToBattery = charging && moving && !engineOn,
            engineToBattery = charging && engineOn
        )
    } else {
        when (t.hybridFlowState) {
            HybridEnergyFlowState.REGEN_BRAKING -> EnergyFlowVisual(wheelsToBattery = true)
            HybridEnergyFlowState.EV_DRIVE -> EnergyFlowVisual(batteryToWheels = true)
            HybridEnergyFlowState.HYBRID_DRIVE -> EnergyFlowVisual(
                engineToWheels = true,
                batteryToWheels = t.throttlePercent >= 60f
            )
            HybridEnergyFlowState.ENGINE_CHARGING -> EnergyFlowVisual(engineToBattery = true)
            HybridEnergyFlowState.IDLE -> EnergyFlowVisual()
        }
    }
}

/** Modo exibido no selo e no texto de status. Sai SEMPRE dos mesmos 4 booleanos que desenham as setas. */
enum class EnergyFlowBadge { NONE, EV, COMBUSTION, EV_PLUS_COMBUSTION, CHARGING }

fun classifyEnergyFlow(flow: EnergyFlowVisual): EnergyFlowBadge = when {
    flow.batteryToWheels && flow.engineToWheels -> EnergyFlowBadge.EV_PLUS_COMBUSTION
    flow.batteryToWheels -> EnergyFlowBadge.EV
    flow.engineToWheels || flow.engineToBattery -> EnergyFlowBadge.COMBUSTION
    flow.wheelsToBattery -> EnergyFlowBadge.CHARGING
    else -> EnergyFlowBadge.NONE
}

private val BadgeEvColor = Color(0xFFFFC21A)
private val BadgeCombustionColor = Color(0xFFFF2A2A)
private val BadgeChargingColor = Color(0xFF35E04A)

private fun badgeAccent(kind: EnergyFlowBadge): Color = when (kind) {
    EnergyFlowBadge.EV -> BadgeEvColor
    EnergyFlowBadge.COMBUSTION -> BadgeCombustionColor
    EnergyFlowBadge.CHARGING -> BadgeChargingColor
    else -> Color.White // caso misto: fundo e borda neutros
}

private fun badgeText(kind: EnergyFlowBadge): AnnotatedString = buildAnnotatedString {
    when (kind) {
        EnergyFlowBadge.EV -> withStyle(SpanStyle(color = BadgeEvColor)) { append("EV") }
        EnergyFlowBadge.COMBUSTION -> withStyle(SpanStyle(color = BadgeCombustionColor)) { append("Combustão") }
        EnergyFlowBadge.CHARGING -> withStyle(SpanStyle(color = BadgeChargingColor)) { append("Carregando") }
        EnergyFlowBadge.EV_PLUS_COMBUSTION -> {
            withStyle(SpanStyle(color = BadgeEvColor)) { append("EV") }
            withStyle(SpanStyle(color = Color.White)) { append(" + ") }
            withStyle(SpanStyle(color = BadgeCombustionColor)) { append("Combustão") }
        }
        EnergyFlowBadge.NONE -> Unit
    }
}

/**
 * Diagrama de Fluxo de Energia idêntico ao painel do Toyota Corolla Hybrid.
 * - Fundo limpo em preto cockpit profundo.
 * - 4 rodas aumentadas com pneus e aros metálicos.
 * - Se não houver fluxo ou ecuLinkOk = false: permanece 100% estático (sem animação de chevrons).
 * - Se houver fluxo: ativa a animação vibrante correspondente.
 */
@Composable
fun CorollaEnergyFlowDiagram(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    val flow = deriveEnergyFlowVisual(telemetry)
    val badge = classifyEnergyFlow(flow)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070B11))
            .border(1.dp, Color(0xFF1E2633), RoundedCornerShape(16.dp))
            .testTag("corolla_energy_flow_diagram")
    ) {
        // Renderiza conteúdo animado apenas quando houver fluxo real; caso contrário estático
        if (flow.hasAnyFlow) {
            AnimatedCorollaEnergyFlowContent(
                telemetry = telemetry,
                flow = flow
            )
        } else {
            StaticCorollaEnergyFlowContent(
                telemetry = telemetry
            )
        }

        // Velocidade Digital Central Superior
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                val speedText = if (telemetry.ecuLinkOk) "${telemetry.speedKmh.toInt()}" else "--"
                Text(
                    text = speedText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km/h",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB0BEC5),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Selo de modo (EV / Combustão / EV + Combustão / Carregando): mesma fonte das setas.
        // Sem fluxo ou sem link com a ECU (flow todo falso): sem selo.
        if (badge != EnergyFlowBadge.NONE) {
            val accent = badgeAccent(badge)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.2f))
                    .border(1.dp, accent, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("energy_mode_badge")
            ) {
                Text(
                    text = badgeText(badge),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Barra inferior: "Exterior xx°C" (PID ambient_temp real) e Status Oficial
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Exterior",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF81D4FA)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val ambientTemp = telemetry.sensors["ambient_temp"]
                val tempText = if (telemetry.ecuLinkOk && ambientTemp != null && !ambientTemp.isNaN()) {
                    "${ambientTemp.roundToInt()}°C"
                } else {
                    "--°C"
                }
                Text(
                    text = tempText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE1F5FE)
                )
            }

            // Status do rodapé usa a mesma classificação do selo, para nunca divergirem
            when {
                !telemetry.ecuLinkOk -> Text(
                    text = "Sem dados",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF90A4AE)
                )
                badge == EnergyFlowBadge.NONE -> Text(
                    text = "Sistema Pronto",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF81D4FA)
                )
                else -> Text(
                    text = badgeText(badge),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Conteúdo estático: sem animação, apenas trilhos apagados, sem chevrons, sem brilho ativo.
 */
@Composable
private fun StaticCorollaEnergyFlowContent(
    telemetry: TelemetryData
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawSpeedometerArc(w, h, if (telemetry.ecuLinkOk) telemetry.speedKmh else 0f)

        val centerX = w * 0.50f
        val frontAxleY = h * 0.38f
        val rearAxleY = h * 0.70f
        val halfTrack = w * 0.20f

        val frontLeftWheel = Offset(centerX - halfTrack, frontAxleY)
        val frontRightWheel = Offset(centerX + halfTrack, frontAxleY)
        val rearLeftWheel = Offset(centerX - halfTrack, rearAxleY)
        val rearRightWheel = Offset(centerX + halfTrack, rearAxleY)

        val enginePos = Offset(centerX, h * 0.21f)
        val batteryCenterY = h * 0.70f
        val batteryWidth = w * 0.26f
        val batteryHeight = 15.dp.toPx()
        val batteryRect = Rect(
            centerX - batteryWidth / 2f,
            batteryCenterY - batteryHeight / 2f,
            centerX + batteryWidth / 2f,
            batteryCenterY + batteryHeight / 2f
        )

        // Trilhos apagados
        drawBaseRails(
            centerX = centerX,
            engineY = enginePos.y + 14.dp.toPx(),
            frontAxleY = frontAxleY,
            batteryTopY = batteryRect.top,
            frontLeft = frontLeftWheel,
            frontRight = frontRightWheel
        )

        // Bloco do motor
        val isEngineOn = telemetry.ecuLinkOk && telemetry.isEngineRunning && telemetry.rpm > 400f
        drawEngineBlock(center = enginePos, isRunning = isEngineOn)

        // 4 Rodas
        drawWheel(frontLeftWheel, isFront = true)
        drawWheel(frontRightWheel, isFront = true)
        drawWheel(rearLeftWheel, isFront = false)
        drawWheel(rearRightWheel, isFront = false)

        // Bateria (8 segmentos apagados se não houver leitura)
        drawCorollaBatteryPack(
            rect = batteryRect,
            socAvailable = telemetry.ecuLinkOk && telemetry.hybridSocAvailable,
            socPercent = telemetry.hybridBatterySocPercent
        )

        drawBatterySymbol(center = Offset(centerX, batteryRect.bottom + 14.dp.toPx()))
    }
}

/**
 * Conteúdo animado: acionado apenas quando há fluxo ativo de energia.
 */
@Composable
private fun AnimatedCorollaEnergyFlowContent(
    telemetry: TelemetryData,
    flow: EnergyFlowVisual
) {
    val infiniteTransition = rememberInfiniteTransition(label = "corolla_flow_anim")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawSpeedometerArc(w, h, if (telemetry.ecuLinkOk) telemetry.speedKmh else 0f)

        val centerX = w * 0.50f
        val frontAxleY = h * 0.38f
        val rearAxleY = h * 0.70f
        val halfTrack = w * 0.20f

        val frontLeftWheel = Offset(centerX - halfTrack, frontAxleY)
        val frontRightWheel = Offset(centerX + halfTrack, frontAxleY)
        val rearLeftWheel = Offset(centerX - halfTrack, rearAxleY)
        val rearRightWheel = Offset(centerX + halfTrack, rearAxleY)

        val enginePos = Offset(centerX, h * 0.21f)
        val batteryCenterY = h * 0.70f
        val batteryWidth = w * 0.26f
        val batteryHeight = 15.dp.toPx()
        val batteryRect = Rect(
            centerX - batteryWidth / 2f,
            batteryCenterY - batteryHeight / 2f,
            centerX + batteryWidth / 2f,
            batteryCenterY + batteryHeight / 2f
        )

        // Canais ativos e setas animadas
        drawActiveEnergyChannels(
            centerX = centerX,
            engineY = enginePos.y + 14.dp.toPx(),
            frontAxleY = frontAxleY,
            batteryTopY = batteryRect.top,
            frontLeft = frontLeftWheel,
            frontRight = frontRightWheel,
            flow = flow,
            flowPhase = flowPhase
        )

        // Bloco do motor
        val isEngineOn = telemetry.ecuLinkOk && telemetry.isEngineRunning && telemetry.rpm > 400f
        drawEngineBlock(center = enginePos, isRunning = isEngineOn)

        // 4 Rodas
        drawWheel(frontLeftWheel, isFront = true)
        drawWheel(frontRightWheel, isFront = true)
        drawWheel(rearLeftWheel, isFront = false)
        drawWheel(rearRightWheel, isFront = false)

        // Bateria
        drawCorollaBatteryPack(
            rect = batteryRect,
            socAvailable = telemetry.ecuLinkOk && telemetry.hybridSocAvailable,
            socPercent = telemetry.hybridBatterySocPercent
        )

        drawBatterySymbol(center = Offset(centerX, batteryRect.bottom + 14.dp.toPx()))
    }
}

/** Trilhos apagados base */
private fun DrawScope.drawBaseRails(
    centerX: Float,
    engineY: Float,
    frontAxleY: Float,
    batteryTopY: Float,
    frontLeft: Offset,
    frontRight: Offset
) {
    val channelWidth = 13.dp.toPx()
    val wheelOffset = 14.dp.toPx()
    val railColor = Color(0xFF141F2C)
    val railBorderColor = Color(0xFF243346)

    // Eixo dianteiro
    drawLine(
        color = railColor,
        start = Offset(frontLeft.x + wheelOffset, frontAxleY),
        end = Offset(frontRight.x - wheelOffset, frontAxleY),
        strokeWidth = channelWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = railBorderColor,
        start = Offset(frontLeft.x + wheelOffset, frontAxleY),
        end = Offset(frontRight.x - wheelOffset, frontAxleY),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Coluna vertical (Motor até Bateria)
    drawLine(
        color = railColor,
        start = Offset(centerX, engineY),
        end = Offset(centerX, batteryTopY),
        strokeWidth = channelWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = railBorderColor,
        start = Offset(centerX, engineY),
        end = Offset(centerX, batteryTopY),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/**
 * Desenha os canais ativos e setas animadas de acordo com as 4 flags puras:
 * - engineToWheels (Vermelho #FF1744)
 * - batteryToWheels (Amarelo #FFD600)
 * - wheelsToBattery (Verde #00E676)
 * - engineToBattery (Vermelho #FF1744)
 * - Se engineToWheels E batteryToWheels: duas barras paralelas (vermelha e amarela) no eixo dianteiro.
 */
private fun DrawScope.drawActiveEnergyChannels(
    centerX: Float,
    engineY: Float,
    frontAxleY: Float,
    batteryTopY: Float,
    frontLeft: Offset,
    frontRight: Offset,
    flow: EnergyFlowVisual,
    flowPhase: Float
) {
    val wheelOffset = 14.dp.toPx()
    val chevronSize = 15.dp.toPx()

    // 1. Desenha trilhos base
    drawBaseRails(centerX, engineY, frontAxleY, batteryTopY, frontLeft, frontRight)

    val colorRed = Color(0xFFFF1744)
    val colorYellow = Color(0xFFFFD600)
    val colorGreen = Color(0xFF00E676)

    // 2. Halo de brilho central se houver fluxo ativo
    val haloColor = when {
        flow.wheelsToBattery -> colorGreen
        flow.engineToWheels && flow.batteryToWheels -> Color(0xFFFF9100)
        flow.batteryToWheels -> colorYellow
        else -> colorRed
    }
    val glowBrush = Brush.radialGradient(
        colors = listOf(haloColor.copy(alpha = 0.38f), Color.Transparent),
        center = Offset(centerX, frontAxleY),
        radius = 90.dp.toPx()
    )
    drawCircle(brush = glowBrush, radius = 90.dp.toPx(), center = Offset(centerX, frontAxleY))

    // 3. CENÁRIO: HÍBRIDO COMPLETO (Combustão + Elétrico para as rodas)
    if (flow.engineToWheels && flow.batteryToWheels) {
        val barOffset = 4.5.dp.toPx()
        val barStroke = 6.dp.toPx()

        // Barra superior vermelha (motor para rodas)
        val redAxleY = frontAxleY - barOffset
        drawLine(
            color = colorRed.copy(alpha = 0.85f),
            start = Offset(frontLeft.x + wheelOffset, redAxleY),
            end = Offset(frontRight.x - wheelOffset, redAxleY),
            strokeWidth = barStroke,
            cap = StrokeCap.Round
        )
        // Setas vermelhas saindo do centro para as rodas
        drawFlowingChevrons(
            start = Offset(centerX, redAxleY),
            end = Offset(frontLeft.x + wheelOffset, redAxleY),
            flowPhase = flowPhase,
            color = colorRed,
            arrowSize = chevronSize * 0.75f,
            numChevrons = 3
        )
        drawFlowingChevrons(
            start = Offset(centerX, redAxleY),
            end = Offset(frontRight.x - wheelOffset, redAxleY),
            flowPhase = flowPhase,
            color = colorRed,
            arrowSize = chevronSize * 0.75f,
            numChevrons = 3
        )

        // Barra inferior amarela (bateria para rodas)
        val yellowAxleY = frontAxleY + barOffset
        drawLine(
            color = colorYellow.copy(alpha = 0.85f),
            start = Offset(frontLeft.x + wheelOffset, yellowAxleY),
            end = Offset(frontRight.x - wheelOffset, yellowAxleY),
            strokeWidth = barStroke,
            cap = StrokeCap.Round
        )
        // Setas amarelas saindo do centro para as rodas
        drawFlowingChevrons(
            start = Offset(centerX, yellowAxleY),
            end = Offset(frontLeft.x + wheelOffset, yellowAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize * 0.75f,
            numChevrons = 3
        )
        drawFlowingChevrons(
            start = Offset(centerX, yellowAxleY),
            end = Offset(frontRight.x - wheelOffset, yellowAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize * 0.75f,
            numChevrons = 3
        )

        // Coluna vertical superior: motor descendo para o eixo (vermelho)
        drawLine(
            color = colorRed.copy(alpha = 0.85f),
            start = Offset(centerX, engineY),
            end = Offset(centerX, frontAxleY),
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, engineY),
            end = Offset(centerX, frontAxleY),
            flowPhase = flowPhase,
            color = colorRed,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 2
        )

        // Coluna vertical inferior: bateria subindo para o eixo (amarelo)
        drawLine(
            color = colorYellow.copy(alpha = 0.85f),
            start = Offset(centerX, batteryTopY),
            end = Offset(centerX, frontAxleY),
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, batteryTopY),
            end = Offset(centerX, frontAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 3
        )

        // Se simultaneamente carrega bateria pelo motor
        if (flow.engineToBattery) {
            drawLine(
                color = colorRed.copy(alpha = 0.6f),
                start = Offset(centerX - 3.dp.toPx(), engineY),
                end = Offset(centerX - 3.dp.toPx(), batteryTopY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        return
    }

    // 4. CENÁRIO: REGENERAÇÃO (Rodas -> Bateria em Verde)
    if (flow.wheelsToBattery) {
        val channelWidth = 10.dp.toPx()
        // Eixo dianteiro
        drawLine(
            color = colorGreen.copy(alpha = 0.85f),
            start = Offset(frontLeft.x + wheelOffset, frontAxleY),
            end = Offset(frontRight.x - wheelOffset, frontAxleY),
            strokeWidth = channelWidth,
            cap = StrokeCap.Round
        )
        // Chevrons convergindo das rodas para o centro
        drawFlowingChevrons(
            start = Offset(frontLeft.x + wheelOffset, frontAxleY),
            end = Offset(centerX, frontAxleY),
            flowPhase = flowPhase,
            color = colorGreen,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 3
        )
        drawFlowingChevrons(
            start = Offset(frontRight.x - wheelOffset, frontAxleY),
            end = Offset(centerX, frontAxleY),
            flowPhase = flowPhase,
            color = colorGreen,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 3
        )

        // Coluna descendo do eixo dianteiro para a bateria
        drawLine(
            color = colorGreen.copy(alpha = 0.85f),
            start = Offset(centerX, frontAxleY),
            end = Offset(centerX, batteryTopY),
            strokeWidth = channelWidth,
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, frontAxleY),
            end = Offset(centerX, batteryTopY),
            flowPhase = flowPhase,
            color = colorGreen,
            arrowSize = chevronSize,
            numChevrons = 4
        )
        return
    }

    // 5. CENÁRIO: MODO EV (Bateria -> Rodas em Amarelo)
    if (flow.batteryToWheels) {
        val channelWidth = 10.dp.toPx()
        // Coluna subindo da bateria para o eixo dianteiro
        drawLine(
            color = colorYellow.copy(alpha = 0.85f),
            start = Offset(centerX, batteryTopY),
            end = Offset(centerX, frontAxleY),
            strokeWidth = channelWidth,
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, batteryTopY),
            end = Offset(centerX, frontAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize,
            numChevrons = 4
        )

        // Eixo dianteiro distribuindo para as rodas
        drawLine(
            color = colorYellow.copy(alpha = 0.85f),
            start = Offset(frontLeft.x + wheelOffset, frontAxleY),
            end = Offset(frontRight.x - wheelOffset, frontAxleY),
            strokeWidth = channelWidth,
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, frontAxleY),
            end = Offset(frontLeft.x + wheelOffset, frontAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 3
        )
        drawFlowingChevrons(
            start = Offset(centerX, frontAxleY),
            end = Offset(frontRight.x - wheelOffset, frontAxleY),
            flowPhase = flowPhase,
            color = colorYellow,
            arrowSize = chevronSize * 0.85f,
            numChevrons = 3
        )
        return
    }

    // 6. CENÁRIO: MOTOR A COMBUSTÃO -> RODAS E/OU CARGA (Vermelho)
    if (flow.engineToWheels || flow.engineToBattery) {
        val channelWidth = 10.dp.toPx()

        if (flow.engineToWheels) {
            // Eixo dianteiro
            drawLine(
                color = colorRed.copy(alpha = 0.85f),
                start = Offset(frontLeft.x + wheelOffset, frontAxleY),
                end = Offset(frontRight.x - wheelOffset, frontAxleY),
                strokeWidth = channelWidth,
                cap = StrokeCap.Round
            )
            drawFlowingChevrons(
                start = Offset(centerX, frontAxleY),
                end = Offset(frontLeft.x + wheelOffset, frontAxleY),
                flowPhase = flowPhase,
                color = colorRed,
                arrowSize = chevronSize * 0.85f,
                numChevrons = 3
            )
            drawFlowingChevrons(
                start = Offset(centerX, frontAxleY),
                end = Offset(frontRight.x - wheelOffset, frontAxleY),
                flowPhase = flowPhase,
                color = colorRed,
                arrowSize = chevronSize * 0.85f,
                numChevrons = 3
            )
        }

        // Coluna vertical vermelha descendo do motor
        val bottomTarget = if (flow.engineToBattery) batteryTopY else frontAxleY
        drawLine(
            color = colorRed.copy(alpha = 0.85f),
            start = Offset(centerX, engineY),
            end = Offset(centerX, bottomTarget),
            strokeWidth = channelWidth,
            cap = StrokeCap.Round
        )
        drawFlowingChevrons(
            start = Offset(centerX, engineY),
            end = Offset(centerX, bottomTarget),
            flowPhase = flowPhase,
            color = colorRed,
            arrowSize = chevronSize,
            numChevrons = if (flow.engineToBattery) 4 else 2
        )
    }
}

/** Arco do velocímetro superior com marcações 100 .. 120 .. 140 */
private fun DrawScope.drawSpeedometerArc(w: Float, h: Float, speedKmh: Float) {
    val arcCenter = Offset(w * 0.50f, h * 0.28f)
    val radius = w * 0.44f

    drawArc(
        color = Color(0xFF1E2838),
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    for (deg in 210..330 step 15) {
        val rad = Math.toRadians(deg.toDouble())
        val innerR = radius - 5.dp.toPx()
        val outerR = radius + 3.dp.toPx()
        val start = Offset(
            (arcCenter.x + cos(rad) * innerR).toFloat(),
            (arcCenter.y + sin(rad) * innerR).toFloat()
        )
        val end = Offset(
            (arcCenter.x + cos(rad) * outerR).toFloat(),
            (arcCenter.y + sin(rad) * outerR).toFloat()
        )
        drawLine(
            color = Color(0xFF455A64),
            start = start,
            end = end,
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

/** Motor a combustão (bloco metálico com aletas superiores do cluster Toyota) */
private fun DrawScope.drawEngineBlock(center: Offset, isRunning: Boolean) {
    val blockW = 42.dp.toPx()
    val blockH = 28.dp.toPx()
    val left = center.x - blockW / 2f
    val top = center.y - blockH / 2f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = if (isRunning) listOf(Color(0xFFECEFF1), Color(0xFF90A4AE))
            else listOf(Color(0xFF546E7A), Color(0xFF37474F)),
            startY = top,
            endY = top + blockH
        ),
        topLeft = Offset(left, top),
        size = Size(blockW, blockH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = if (isRunning) Color(0xFFFFFFFF) else Color(0xFF78909C),
        topLeft = Offset(left, top),
        size = Size(blockW, blockH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    for (i in 1..3) {
        val y = top + (blockH * i / 4f)
        drawLine(
            color = if (isRunning) Color(0xFF455A64) else Color(0xFF263238),
            start = Offset(left + 6.dp.toPx(), y),
            end = Offset(left + blockW - 6.dp.toPx(), y),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/** Desenha roda AUMENTADA, com pneus largos em relevo e aro de liga prateado */
private fun DrawScope.drawWheel(pos: Offset, isFront: Boolean) {
    val wheelW = 20.dp.toPx()
    val wheelH = 46.dp.toPx()
    val left = pos.x - wheelW / 2f
    val top = pos.y - wheelH / 2f

    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF10141A), Color(0xFF2D3748), Color(0xFF10141A)),
            startX = left,
            endX = left + wheelW
        ),
        topLeft = Offset(left, top),
        size = Size(wheelW, wheelH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF4A5568),
        topLeft = Offset(left, top),
        size = Size(wheelW, wheelH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    for (i in 1..4) {
        val treadY = top + (wheelH * i / 5f)
        drawLine(
            color = Color(0xFF1A202C),
            start = Offset(left + 2.dp.toPx(), treadY),
            end = Offset(left + wheelW - 2.dp.toPx(), treadY),
            strokeWidth = 1.2.dp.toPx()
        )
    }

    val rimW = 9.dp.toPx()
    val rimH = 26.dp.toPx()
    val rimTop = top + (wheelH - rimH) / 2f
    val rimLeft = pos.x - rimW / 2f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFECEFF1), Color(0xFF78909C), Color(0xFFCFD8DC)),
            startY = rimTop,
            endY = rimTop + rimH
        ),
        topLeft = Offset(rimLeft, rimTop),
        size = Size(rimW, rimH),
        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
    )

    drawCircle(
        color = Color(0xFF263238),
        radius = 2.5.dp.toPx(),
        center = pos
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 1.2.dp.toPx(),
        center = pos
    )
}

/**
 * Bateria de 8 Segmentos retangulares idêntica à do cluster do Corolla.
 * Se socAvailable = false, desenha todos os 8 segmentos apagados.
 * Se socAvailable = true, ilumina round(soc / 100 * 8) segmentos em ciano vivo (#00E5FF).
 */
private fun DrawScope.drawCorollaBatteryPack(
    rect: Rect,
    socAvailable: Boolean,
    socPercent: Float
) {
    drawRoundRect(
        color = Color(0xFF101722),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Fill
    )
    drawRoundRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    val totalSegments = 8
    val segmentSpacing = 1.5.dp.toPx()
    val totalSpacing = segmentSpacing * (totalSegments + 1)
    val segmentWidth = (rect.width - totalSpacing) / totalSegments
    val segmentHeight = rect.height - 3.dp.toPx()
    val segmentTop = rect.top + 1.5.dp.toPx()

    val litSegments = if (socAvailable) {
        ((socPercent / 100f) * totalSegments).roundToInt().coerceIn(0, totalSegments)
    } else {
        0
    }

    for (i in 0 until totalSegments) {
        val segLeft = rect.left + segmentSpacing + i * (segmentWidth + segmentSpacing)
        val isLit = i < litSegments

        if (isLit) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)),
                    startY = segmentTop,
                    endY = segmentTop + segmentHeight
                ),
                topLeft = Offset(segLeft, segmentTop),
                size = Size(segmentWidth, segmentHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.6f),
                topLeft = Offset(segLeft, segmentTop),
                size = Size(segmentWidth, segmentHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                style = Stroke(width = 0.6.dp.toPx())
            )
        } else {
            drawRoundRect(
                color = Color(0xFF1E2838),
                topLeft = Offset(segLeft, segmentTop),
                size = Size(segmentWidth, segmentHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                style = Stroke(width = 0.6.dp.toPx())
            )
        }
    }
}

/** Símbolo de bateria (+/-) abaixo do pacote de células */
private fun DrawScope.drawBatterySymbol(center: Offset) {
    val bodyW = 15.dp.toPx()
    val bodyH = 8.dp.toPx()
    val left = center.x - bodyW / 2f
    val top = center.y - bodyH / 2f

    drawRoundRect(
        color = Color(0xFF90A4AE),
        topLeft = Offset(left, top),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        style = Stroke(width = 1.1.dp.toPx())
    )
    drawRect(
        color = Color(0xFF90A4AE),
        topLeft = Offset(left + bodyW, top + 2.dp.toPx()),
        size = Size(2.dp.toPx(), 4.dp.toPx())
    )
    val plusX = left + 5.dp.toPx()
    val plusY = center.y
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(plusX - 2.5.dp.toPx(), plusY),
        end = Offset(plusX + 2.5.dp.toPx(), plusY),
        strokeWidth = 1.2.dp.toPx()
    )
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(plusX, plusY - 2.5.dp.toPx()),
        end = Offset(plusX, plusY + 2.5.dp.toPx()),
        strokeWidth = 1.2.dp.toPx()
    )
    val minusX = left + bodyW - 6.dp.toPx()
    val minusY = center.y
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(minusX - 2.5.dp.toPx(), minusY),
        end = Offset(minusX + 2.5.dp.toPx(), minusY),
        strokeWidth = 1.2.dp.toPx()
    )
}

/** Desenha setas/chevrons luminosas viajando ao longo da linha de fluxo */
private fun DrawScope.drawFlowingChevrons(
    start: Offset,
    end: Offset,
    flowPhase: Float,
    color: Color,
    arrowSize: Float,
    numChevrons: Int
) {
    val dir = end - start
    val totalLen = dir.getDistance()
    if (totalLen <= 1f) return
    val unitX = dir.x / totalLen
    val unitY = dir.y / totalLen

    val perpX = -unitY
    val perpY = unitX

    val spacing = 1.0f / numChevrons

    for (i in 0 until numChevrons) {
        val fraction = (i * spacing + flowPhase) % 1.0f
        if (fraction !in 0.05f..0.95f) continue

        val tipX = start.x + unitX * (totalLen * fraction)
        val tipY = start.y + unitY * (totalLen * fraction)

        val wingLen = arrowSize * 0.95f
        val wingSpan = arrowSize * 0.70f

        val leftX = tipX - unitX * wingLen + perpX * wingSpan
        val leftY = tipY - unitY * wingLen + perpY * wingSpan

        val rightX = tipX - unitX * wingLen - perpX * wingSpan
        val rightY = tipY - unitY * wingLen - perpY * wingSpan

        val notchX = tipX - unitX * (wingLen * 0.45f)
        val notchY = tipY - unitY * (wingLen * 0.45f)

        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(leftX, leftY)
            lineTo(notchX, notchY)
            lineTo(rightX, rightY)
            close()
        }

        drawPath(
            path = arrowPath,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(path = arrowPath, color = Color.White)
        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
