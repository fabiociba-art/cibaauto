package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EnergyMode
import com.example.model.PreloadedVehicles
import com.example.model.TelemetryState
import com.example.model.VehicleProfile
import com.example.model.deriveEnergyMode
import com.example.model.RPM_ENGINE_ON_THRESHOLD
import com.example.ui.theme.*
import kotlin.math.*

val EnergyMode.badgeColor: Color
    get() = Color(this.colorArgb)

/** Retorna a cor correspondente à faixa de carga da bateria HV */
fun getSocColor(soc: Int): Color {
    return when {
        soc < 20 -> StatusRed
        soc <= 50 -> StatusAmber
        else -> Color(0xFF00FF66)
    }
}

// ============================================================================
// TELA PRINCIPAL: FLUXO DE ENERGIA DO CARRO HÍBRIDO
// ============================================================================
@Composable
fun EnergyFlowScreen(
    telemetry: TelemetryState,
    vehicle: VehicleProfile = PreloadedVehicles.corollaGliHybrid2026,
    isPowerInverted: Boolean = false,
    onToggleDemo: () -> Unit = {},
    onTogglePowerInvert: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val rawMode = remember(telemetry, vehicle.isHybrid, isPowerInverted) {
        deriveEnergyMode(telemetry, vehicle.isHybrid, isPowerInverted)
    }

    // Histerese suave para evitar cintilação em leituras oscilantes de OBD
    var smoothedMode by remember { mutableStateOf(rawMode) }
    LaunchedEffect(rawMode) {
        // Modo demo responde imediatamente; modo real suaviza em 200ms
        if (telemetry.isDemoMode) {
            smoothedMode = rawMode
        } else {
            kotlinx.coroutines.delay(200)
            smoothedMode = rawMode
        }
    }

    val effectiveKw = if (isPowerInverted) -telemetry.hybridMotorPowerKw else telemetry.hybridMotorPowerKw

    // Transição infinita para partículas e pista de luz diagonal
    val infiniteTransition = rememberInfiniteTransition(label = "EnergyFlowAnim")

    // Fase das partículas de energia ao longo dos tubos condutores
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = calculateFlowDuration(smoothedMode, effectiveKw, telemetry.speed),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticlesPhase"
    )

    // Fase de movimento das listras de luz da pista diagonal
    val roadPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = calculateRoadDuration(telemetry.speed),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RoadStripesPhase"
    )

    // Pulso suave para modo standby
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StandbyPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBlack)
    ) {
        // Fundo com gradiente radial sutil de cockpit esportivo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0C1420), CockpitBlack),
                        radius = 1200f
                    )
                )
        )

        if (isLandscape) {
            // Layout Paisagem / Tablet (Duas colunas lado a lado)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Coluna Esquerda: Pista diagonal + Desenho vetorial do Carro
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    color = CockpitSurface,
                    border = BorderStroke(1.dp, CockpitCardBorder)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EnergyFlowCockpitCanvas(
                            telemetry = telemetry,
                            mode = smoothedMode,
                            vehicle = vehicle,
                            effectiveKw = effectiveKw,
                            flowPhase = flowPhase,
                            roadPhase = roadPhase,
                            pulsePhase = pulsePhase,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Badge do Modo no topo do Canvas
                        ModeBadgeOverlay(
                            mode = smoothedMode,
                            isDemo = telemetry.isDemoMode,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                        )
                    }
                }

                // Coluna Direita: Dados de Direção, Bateria e Telemetria
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CorollaTripHeader(telemetry = telemetry, mode = smoothedMode)
                    HvBatterySocCard(telemetry = telemetry)
                    PowertrainStatsCard(telemetry = telemetry, effectiveKw = effectiveKw, mode = smoothedMode)
                    ScreenActionButtons(
                        isDemo = telemetry.isDemoMode,
                        isPowerInverted = isPowerInverted,
                        onToggleDemo = onToggleDemo,
                        onTogglePowerInvert = onTogglePowerInvert
                    )
                }
            }
        } else {
            // Layout Retrato (Smartphone Vertical)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header estilo multimídia do Corolla ("Dados de Direção")
                CorollaTripHeader(telemetry = telemetry, mode = smoothedMode)

                // 2. Quadro central do cockpit com Carro Vetorial + Pista Neon Diagonal + Animação de Fluxo
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CockpitSurface,
                    border = BorderStroke(1.5.dp, Color(0xFF1E2C44))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EnergyFlowCockpitCanvas(
                            telemetry = telemetry,
                            mode = smoothedMode,
                            vehicle = vehicle,
                            effectiveKw = effectiveKw,
                            flowPhase = flowPhase,
                            roadPhase = roadPhase,
                            pulsePhase = pulsePhase,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Status do modo em destaque sobreposto no canto superior
                        ModeBadgeOverlay(
                            mode = smoothedMode,
                            isDemo = telemetry.isDemoMode,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        )

                        // Indicador de modo de condução ("ECO" / "EV") no canto direito
                        DriveModePill(
                            mode = smoothedMode,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        )
                    }
                }

                // 3. Indicador de Bateria de Alta Tensão (HV)
                if (vehicle.isHybrid) {
                    HvBatterySocCard(telemetry = telemetry)
                }

                // 4. Métricas do Trem de Força
                PowertrainStatsCard(telemetry = telemetry, effectiveKw = effectiveKw, mode = smoothedMode)

                // 5. Botões de ação e modo de demonstração
                ScreenActionButtons(
                    isDemo = telemetry.isDemoMode,
                    isPowerInverted = isPowerInverted,
                    onToggleDemo = onToggleDemo,
                    onTogglePowerInvert = onTogglePowerInvert
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ============================================================================
// HEADER ESTILO MULTIMÍDIA TOYOTA COROLLA ("DADOS DE DIREÇÃO")
// ============================================================================
@Composable
private fun CorollaTripHeader(telemetry: TelemetryState, mode: EnergyMode) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF090F19),
        border = BorderStroke(1.dp, Color(0xFF162338)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Título superior e badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "FLUXO DE ENERGIA",
                        color = Color(0xFF00FFCC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                DriveModePill(mode = mode)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Indicadores da referência: Velocidade Média, Tempo Decorrido e Autonomia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Velocidade Média
                HeaderMetricItem(
                    label = "VELOC. MÉDIA",
                    value = if (telemetry.tripAvgSpeed > 0f) String.format("%.1f", telemetry.tripAvgSpeed) else if (telemetry.speed > 0f) String.format("%.0f", telemetry.speed) else "0.0",
                    unit = "km/h",
                    accentColor = Color(0xFF00E5FF)
                )

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFF1B2A40))
                )

                // 2. Tempo Decorrido
                val totalSec = telemetry.tripDurationSec
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                val timeFormatted = if (hours > 0) String.format("%02d:%02d:%02d", hours, mins, secs) else String.format("%02d:%02d", mins, secs)

                HeaderMetricItem(
                    label = "TEMPO DECORRIDO",
                    value = timeFormatted,
                    unit = "",
                    accentColor = Color(0xFFF8FAFC)
                )

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFF1B2A40))
                )

                // 3. Autonomia
                val rangeStr = if (telemetry.rangeKm > 0) "${telemetry.rangeKm}" else "—"
                HeaderMetricItem(
                    label = "AUTONOMIA",
                    value = rangeStr,
                    unit = "km",
                    accentColor = Color(0xFF00FF66)
                )
            }
        }
    }
}

@Composable
private fun HeaderMetricItem(
    label: String,
    value: String,
    unit: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = accentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun DriveModePill(mode: EnergyMode, modifier: Modifier = Modifier) {
    val (text, color) = when (mode) {
        EnergyMode.EV -> "EV MODE" to Color(0xFF00F0FF)
        EnergyMode.CHARGING -> "ECO / REGEN" to Color(0xFF00FF66)
        EnergyMode.HYBRID -> "POWER" to Color(0xFF00FFCC)
        EnergyMode.ENGINE -> "NORMAL" to Color(0xFFFF9100)
        EnergyMode.STANDBY -> "ECO" to Color(0xFF10B981)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModeBadgeOverlay(mode: EnergyMode, isDemo: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF060B14).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, mode.badgeColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(mode.badgeColor, CircleShape)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isDemo) "MODO DEMO • ${mode.displayName}" else "MODO ${mode.displayName}",
                    color = mode.badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = mode.subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

// ============================================================================
// CANVAS CENTRAL: CARRO VETORIAL + PISTA DE LUZ DIAGONAL + FLUXO ANIMADO
// ============================================================================
@Composable
private fun EnergyFlowCockpitCanvas(
    telemetry: TelemetryState,
    mode: EnergyMode,
    vehicle: VehicleProfile,
    effectiveKw: Float,
    flowPhase: Float,
    roadPhase: Float,
    pulsePhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. DESENHO DA FAIXA VERDE DIAGONAL (ESTRADA DE LUZ DO VÍDEO DA MULTIMÍDIA)
        drawDiagonalGreenHighway(w, h, roadPhase, telemetry.speed)

        // 2. DIMENSÕES E CENTRO DO CARRO
        val carCenterX = w * 0.50f
        val carCenterY = h * 0.52f
        val carWidth = min(w * 0.44f, 175.dp.toPx())
        val carHeight = carWidth * 2.25f

        // Coordenadas calculadas dos componentes principais sobre o carro
        val engineY = carCenterY - carHeight * 0.30f
        val motorY = carCenterY - carHeight * 0.12f
        val batteryY = carCenterY + carHeight * 0.24f
        val frontAxleY = carCenterY - carHeight * 0.22f
        val rearAxleY = carCenterY + carHeight * 0.26f

        val leftFrontWheelX = carCenterX - carWidth * 0.50f
        val rightFrontWheelX = carCenterX + carWidth * 0.50f
        val leftRearWheelX = carCenterX - carWidth * 0.50f
        val rightRearWheelX = carCenterX + carWidth * 0.50f

        // 3. DESENHO DO CARRO (SILHUETA SEDÃ BRANCO TOYOTA COROLLA COM CONTORNO NEON)
        drawStylizedWhiteSedan(
            cx = carCenterX,
            cy = carCenterY,
            width = carWidth,
            height = carHeight,
            speed = telemetry.speed,
            mode = mode
        )

        // 4. DESENHO DOS CONDUTOS E FLUXO ANIMADO DE ENERGIA (SEGUNDO O MODO ATIVO)
        drawEnergyFlowStreams(
            cx = carCenterX,
            engineY = engineY,
            motorY = motorY,
            batteryY = batteryY,
            frontAxleY = frontAxleY,
            leftWheelX = leftFrontWheelX,
            rightWheelX = rightFrontWheelX,
            mode = mode,
            isHybrid = vehicle.isHybrid,
            flowPhase = flowPhase,
            effectiveKw = effectiveKw,
            pulsePhase = pulsePhase
        )

        // 5. DESENHO DOS COMPONENTES SOBRE O CARRO
        // a) Motor a Combustão (Frente)
        drawCombustionEngineNode(
            cx = carCenterX,
            cy = engineY,
            rpm = telemetry.rpm,
            isActive = telemetry.rpm > RPM_ENGINE_ON_THRESHOLD,
            pulse = if (mode == EnergyMode.ENGINE || mode == EnergyMode.HYBRID) pulsePhase else 1f
        )

        // b) Motor Elétrico / Gerador (Centro-Frente) - se veículo for híbrido
        if (vehicle.isHybrid) {
            drawElectricMotorNode(
                cx = carCenterX,
                cy = motorY,
                effectiveKw = effectiveKw,
                mode = mode,
                pulse = pulsePhase
            )

            // c) Bateria de Alta Tensão HV (Traseira)
            drawHighVoltageBatteryNode(
                cx = carCenterX,
                cy = batteryY,
                soc = telemetry.hybridBatterySoc,
                mode = mode,
                pulse = if (mode == EnergyMode.CHARGING) pulsePhase else 1f
            )
        }

        // d) Rodas Dianteiras e Traseiras com detalhes de tração
        drawWheelNodes(
            leftX = leftFrontWheelX,
            rightX = rightFrontWheelX,
            y = frontAxleY,
            speed = telemetry.speed,
            isFront = true,
            isDriven = true
        )
        drawWheelNodes(
            leftX = leftRearWheelX,
            rightX = rightRearWheelX,
            y = rearAxleY,
            speed = telemetry.speed,
            isFront = false,
            isDriven = false
        )
    }
}

/**
 * Desenha a faixa verde brilhante e inclinada na diagonal, com ondas/listras de luz em movimento contínuo.
 */
private fun DrawScope.drawDiagonalGreenHighway(w: Float, h: Float, roadPhase: Float, speed: Float) {
    // Pista larga inclinada na diagonal atravessando o fundo (como na multimídia do Corolla)
    val highwayPath = Path().apply {
        moveTo(w * -0.2f, h * 0.15f)
        lineTo(w * 0.85f, h * 0.05f)
        lineTo(w * 1.25f, h * 0.88f)
        lineTo(w * 0.20f, h * 0.98f)
        close()
    }

    // Fundo da faixa verde com gradiente suave
    drawPath(
        path = highwayPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00FFCC).copy(alpha = 0.05f),
                Color(0xFF00FF66).copy(alpha = 0.15f),
                Color(0xFF00E5FF).copy(alpha = 0.06f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
    )

    // Bordas brilhantes da pista (linhas neon verdes)
    drawLine(
        color = Color(0xFF00FFCC).copy(alpha = 0.55f),
        start = Offset(w * -0.2f, h * 0.15f),
        end = Offset(w * 0.20f, h * 0.98f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF00FF66).copy(alpha = 0.55f),
        start = Offset(w * 0.85f, h * 0.05f),
        end = Offset(w * 1.25f, h * 0.88f),
        strokeWidth = 3f
    )

    // Listras/ondas de luz transversais que se movem continuamente para dar a sensação de movimento
    val stripeCount = 9
    for (i in 0 until stripeCount) {
        val t = (i / stripeCount.toFloat() + roadPhase) % 1f
        val startX = lerp(w * -0.2f, w * 0.20f, t)
        val startY = lerp(h * 0.15f, h * 0.98f, t)
        val endX = lerp(w * 0.85f, w * 1.25f, t)
        val endY = lerp(h * 0.05f, h * 0.88f, t)

        val alpha = sin(t * Math.PI.toFloat()) * (if (speed > 1f) 0.40f else 0.18f)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color(0xFF00FFCC).copy(alpha = alpha),
                    Color(0xFF00FF66).copy(alpha = alpha * 1.2f),
                    Color.Transparent
                )
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f
        )
    }
}

/**
 * Desenha a silhueta vetorial estilizada do Corolla sedã branco, com vista superior limpa e moderna.
 */
private fun DrawScope.drawStylizedWhiteSedan(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    speed: Float,
    mode: EnergyMode
) {
    val halfW = width / 2f
    val halfH = height / 2f

    // Sombra suave sob o chassi
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(cx - halfW * 1.08f, cy - halfH * 1.02f),
        size = Size(width * 1.16f, height * 1.04f),
        cornerRadius = CornerRadius(28f, 28f)
    )

    // 1. CARROCERIA PRINCIPAL (Sedã Branco com curvas aerodinâmicas)
    val bodyPath = Path().apply {
        // Para-choque dianteiro aerodinâmico
        moveTo(cx - halfW * 0.65f, cy - halfH)
        quadraticTo(cx, cy - halfH * 1.05f, cx + halfW * 0.65f, cy - halfH)

        // Paralama dianteiro direito
        cubicTo(
            cx + halfW * 0.92f, cy - halfH * 0.85f,
            cx + halfW * 0.98f, cy - halfH * 0.65f,
            cx + halfW * 0.90f, cy - halfH * 0.45f
        )

        // Lateral direita da cabine
        lineTo(cx + halfW * 0.96f, cy + halfH * 0.45f)

        // Paralama traseiro direito
        cubicTo(
            cx + halfW * 1.00f, cy + halfH * 0.65f,
            cx + halfW * 0.92f, cy + halfH * 0.85f,
            cx + halfW * 0.70f, cy + halfH
        )

        // Para-choque traseiro com spoiler
        quadraticTo(cx, cy + halfH * 1.03f, cx - halfW * 0.70f, cy + halfH)

        // Paralama traseiro esquerdo
        cubicTo(
            cx - halfW * 0.92f, cy + halfH * 0.85f,
            cx - halfW * 1.00f, cy + halfH * 0.65f,
            cx - halfW * 0.96f, cy + halfH * 0.45f
        )

        // Lateral esquerda da cabine
        lineTo(cx - halfW * 0.90f, cy - halfH * 0.45f)

        // Paralama dianteiro esquerdo
        cubicTo(
            cx - halfW * 0.98f, cy - halfH * 0.65f,
            cx - halfW * 0.92f, cy - halfH * 0.85f,
            cx - halfW * 0.65f, cy - halfH
        )
        close()
    }

    // Preenchimento do corpo em branco perolizado suave com reflexos
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFECEFF1),
                Color(0xFFE2E8F0),
                Color(0xFFF8FAFC)
            ),
            startY = cy - halfH,
            endY = cy + halfH
        )
    )

    // Contorno neon esportivo do carro
    val neonEdgeColor = when (mode) {
        EnergyMode.EV -> Color(0xFF00F0FF).copy(alpha = 0.85f)
        EnergyMode.CHARGING -> Color(0xFF00FF66).copy(alpha = 0.85f)
        EnergyMode.HYBRID -> Color(0xFF00FFCC).copy(alpha = 0.85f)
        EnergyMode.ENGINE -> Color(0xFFFF9100).copy(alpha = 0.75f)
        EnergyMode.STANDBY -> Color(0xFF64748B).copy(alpha = 0.50f)
    }

    drawPath(
        path = bodyPath,
        color = neonEdgeColor,
        style = Stroke(width = 2.2f)
    )

    // 2. VIDROS E TETO SOLAR (Cabine escurecida de vidro com reflexo ciano)
    val glassPath = Path().apply {
        moveTo(cx - halfW * 0.58f, cy - halfH * 0.35f)
        lineTo(cx + halfW * 0.58f, cy - halfH * 0.35f)
        lineTo(cx + halfW * 0.65f, cy + halfH * 0.35f)
        lineTo(cx - halfW * 0.65f, cy + halfH * 0.35f)
        close()
    }
    drawPath(
        path = glassPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            ),
            startY = cy - halfH * 0.35f,
            endY = cy + halfH * 0.35f
        )
    )
    drawPath(path = glassPath, color = Color(0xFF38BDF8).copy(alpha = 0.4f), style = Stroke(1.5f))

    // Teto solar central
    drawRoundRect(
        color = Color(0xFF0A0F1D),
        topLeft = Offset(cx - halfW * 0.40f, cy - halfH * 0.12f),
        size = Size(halfW * 0.80f, height * 0.24f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Faróis dianteiros LED (Projetores brancos/azulados brilhantes)
    val headlightY = cy - halfH * 0.94f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, Color(0xFF00E5FF), Color.Transparent)),
        radius = 7f,
        center = Offset(cx - halfW * 0.52f, headlightY)
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, Color(0xFF00E5FF), Color.Transparent)),
        radius = 7f,
        center = Offset(cx + halfW * 0.52f, headlightY)
    )

    // Lanternas traseiras LED (Barra de luz vermelha)
    val taillightY = cy + halfH * 0.95f
    drawLine(
        color = Color(0xFFFF1744),
        start = Offset(cx - halfW * 0.60f, taillightY),
        end = Offset(cx + halfW * 0.60f, taillightY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Retrovisores aerodinâmicos laterais
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx - halfW * 1.14f, cy - halfH * 0.38f),
        size = Size(halfW * 0.22f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx + halfW * 0.92f, cy - halfH * 0.38f),
        size = Size(halfW * 0.22f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

/**
 * Desenha os condutos e as partículas animadas de energia entre motor, bateria, gerador e rodas.
 */
private fun DrawScope.drawEnergyFlowStreams(
    cx: Float,
    engineY: Float,
    motorY: Float,
    batteryY: Float,
    frontAxleY: Float,
    leftWheelX: Float,
    rightWheelX: Float,
    mode: EnergyMode,
    isHybrid: Boolean,
    flowPhase: Float,
    effectiveKw: Float,
    pulsePhase: Float
) {
    if (mode == EnergyMode.STANDBY) {
        // Em Standby, desenhamos apenas as conexões em repouso com pulso suave
        val conduitColor = Color(0xFF334155).copy(alpha = 0.45f * pulsePhase)
        drawLine(conduitColor, Offset(cx, engineY), Offset(cx, frontAxleY), strokeWidth = 3f)
        if (isHybrid) {
            drawLine(conduitColor, Offset(cx, motorY), Offset(cx, batteryY), strokeWidth = 3f)
        }
        drawLine(conduitColor, Offset(leftWheelX, frontAxleY), Offset(rightWheelX, frontAxleY), strokeWidth = 3f)
        return
    }

    // ── MODO 1: 100% ELÉTRICO (EV) ──
    // Bateria HV → Motor Elétrico → Rodas dianteiras (Ciano/Azul)
    if (mode == EnergyMode.EV && isHybrid) {
        val streamColor = Color(0xFF00F0FF)

        // Conduto Bateria -> Motor
        drawGlowingStream(
            from = Offset(cx, batteryY),
            to = Offset(cx, motorY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 5,
            width = 5f
        )
        // Conduto Motor -> Eixo Dianteiro
        drawGlowingStream(
            from = Offset(cx, motorY),
            to = Offset(cx, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 5f
        )
        // Divisão do Eixo para as Rodas Esquerda e Direita
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(leftWheelX, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 4.5f
        )
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(rightWheelX, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 4.5f
        )
    }

    // ── MODO 2: COMBUSTÃO PURA ──
    // Motor a Combustão → Rodas dianteiras (Laranja)
    if (mode == EnergyMode.ENGINE) {
        val streamColor = Color(0xFFFF9100)

        // Conduto Motor a combustão -> Eixo
        drawGlowingStream(
            from = Offset(cx, engineY),
            to = Offset(cx, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 4,
            width = 5.5f
        )
        // Eixo para rodas
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(leftWheelX, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 4.5f
        )
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(rightWheelX, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 4.5f
        )
    }

    // ── MODO 3: HÍBRIDO (AMBOS JUNTOS) ──
    // Combustão (Laranja) + Bateria (Ciano) → Rodas dianteiras
    if (mode == EnergyMode.HYBRID && isHybrid) {
        // Stream 1: Bateria -> Motor -> Eixo (Ciano)
        drawGlowingStream(
            from = Offset(cx, batteryY),
            to = Offset(cx, motorY),
            color = Color(0xFF00F0FF),
            phase = flowPhase,
            particleCount = 5,
            width = 5f
        )
        // Stream 2: Motor a Combustão -> Eixo (Laranja)
        drawGlowingStream(
            from = Offset(cx, engineY),
            to = Offset(cx, frontAxleY),
            color = Color(0xFFFF9100),
            phase = (flowPhase + 0.5f) % 1f,
            particleCount = 4,
            width = 5.5f
        )
        // Eixo para rodas (mistura híbrida em Ciano e Laranja)
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(leftWheelX, frontAxleY),
            color = Color(0xFF00FFCC),
            phase = flowPhase,
            particleCount = 4,
            width = 6f
        )
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(rightWheelX, frontAxleY),
            color = Color(0xFF00FFCC),
            phase = flowPhase,
            particleCount = 4,
            width = 6f
        )
    }

    // ── MODO 4: CARREGANDO / REGENERAÇÃO ──
    // Rodas (frenagem) / Combustão → Motor/Gerador → BATERIA (Verde Vibrante)
    // O fluxo precisa ir VISIVELMENTE PARA A BATERIA!
    if (mode == EnergyMode.CHARGING && isHybrid) {
        val streamColor = Color(0xFF00FF66)

        // Fluxo das rodas para o eixo central
        drawGlowingStream(
            from = Offset(leftWheelX, frontAxleY),
            to = Offset(cx, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 5f
        )
        drawGlowingStream(
            from = Offset(rightWheelX, frontAxleY),
            to = Offset(cx, frontAxleY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 5f
        )
        // Do eixo dianteiro para o Motor/Gerador
        drawGlowingStream(
            from = Offset(cx, frontAxleY),
            to = Offset(cx, motorY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 3,
            width = 5.5f
        )
        // Do Motor/Gerador PARA A BATERIA HV! (Sentido inverso)
        drawGlowingStream(
            from = Offset(cx, motorY),
            to = Offset(cx, batteryY),
            color = streamColor,
            phase = flowPhase,
            particleCount = 6,
            width = 6.5f
        )
    }
}

/**
 * Desenha uma linha de conduto com partículas luminosas e setas viajando ao longo dela.
 */
private fun DrawScope.drawGlowingStream(
    from: Offset,
    to: Offset,
    color: Color,
    phase: Float,
    particleCount: Int,
    width: Float
) {
    // 1. Linha base translúcida do conduto
    drawLine(
        color = color.copy(alpha = 0.28f),
        start = from,
        end = to,
        strokeWidth = width,
        cap = StrokeCap.Round
    )

    // Halo externo de néon
    drawLine(
        color = color.copy(alpha = 0.12f),
        start = from,
        end = to,
        strokeWidth = width * 2.2f,
        cap = StrokeCap.Round
    )

    // 2. Partículas viajando ao longo do conduto
    for (i in 0 until particleCount) {
        val fraction = (phase + i.toFloat() / particleCount) % 1f
        val px = lerp(from.x, to.x, fraction)
        val py = lerp(from.y, to.y, fraction)

        // Brilho exterior da partícula
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = width * 1.5f,
            center = Offset(px, py)
        )
        // Núcleo branco quente da partícula
        drawCircle(
            color = Color.White,
            radius = width * 0.7f,
            center = Offset(px, py)
        )

        // Seta direcional estilizada
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = sqrt(dx * dx + dy * dy)
        if (len > 15f) {
            val uX = dx / len
            val uY = dy / len
            val arrowLength = width * 1.8f
            val pX = -uY * (width * 0.8f)
            val pY = uX * (width * 0.8f)

            val head = Offset(px + uX * arrowLength * 0.5f, py + uY * arrowLength * 0.5f)
            val base1 = Offset(px - uX * arrowLength * 0.5f + pX, py - uY * arrowLength * 0.5f + pY)
            val base2 = Offset(px - uX * arrowLength * 0.5f - pX, py - uY * arrowLength * 0.5f - pY)

            val arrowPath = Path().apply {
                moveTo(head.x, head.y)
                lineTo(base1.x, base1.y)
                lineTo(base2.x, base2.y)
                close()
            }
            drawPath(path = arrowPath, color = color.copy(alpha = 0.9f))
        }
    }
}

/**
 * Desenha o nó do Motor a Combustão (1.8L Atkinson).
 */
private fun DrawScope.drawCombustionEngineNode(
    cx: Float,
    cy: Float,
    rpm: Int,
    isActive: Boolean,
    pulse: Float
) {
    val nodeWidth = 84.dp.toPx()
    val nodeHeight = 36.dp.toPx()
    val cornerRadius = CornerRadius(10f, 10f)

    val fillColor = if (isActive) Color(0xFF2A1502) else Color(0xFF131A26)
    val strokeColor = if (isActive) Color(0xFFFF9100).copy(alpha = 0.9f * pulse) else Color(0xFF334155)

    // Fundo do nó
    drawRoundRect(
        color = fillColor,
        topLeft = Offset(cx - nodeWidth / 2f, cy - nodeHeight / 2f),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(cx - nodeWidth / 2f, cy - nodeHeight / 2f),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = if (isActive) 2.2f else 1.2f)
    )

    // Detalhe dos pistões / cilindros
    val cylinderCount = 4
    val startCylX = cx - nodeWidth * 0.35f
    val stepCylX = (nodeWidth * 0.70f) / (cylinderCount - 1)
    for (i in 0 until cylinderCount) {
        val cylX = startCylX + i * stepCylX
        val cylColor = if (isActive) Color(0xFFFF9100) else Color(0xFF475569)
        drawCircle(
            color = cylColor,
            radius = 3.5f,
            center = Offset(cylX, cy - 4f)
        )
    }

    // Indicador de ignição / chamas se ativo
    if (isActive) {
        drawCircle(
            color = Color(0xFFFFAB40).copy(alpha = 0.6f * pulse),
            radius = 6f,
            center = Offset(cx, cy - 4f)
        )
    }
}

/**
 * Desenha o nó do Motor Elétrico / Gerador (MG1 / MG2 do câmbio e-CVT).
 */
private fun DrawScope.drawElectricMotorNode(
    cx: Float,
    cy: Float,
    effectiveKw: Float,
    mode: EnergyMode,
    pulse: Float
) {
    val radius = 22.dp.toPx()
    val nodeColor = when (mode) {
        EnergyMode.CHARGING -> Color(0xFF00FF66)
        EnergyMode.EV, EnergyMode.HYBRID -> Color(0xFF00F0FF)
        else -> Color(0xFF475569)
    }

    // Fundo com bobina eletromagnética
    drawCircle(
        color = Color(0xFF08121E),
        radius = radius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = nodeColor.copy(alpha = if (mode == EnergyMode.STANDBY) 0.35f else 0.85f * pulse),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.4f)
    )

    // Espiras da bobina (anel interno pontilhado)
    val coils = 8
    for (i in 0 until coils) {
        val angle = (i / coils.toFloat()) * (2f * Math.PI.toFloat())
        val coilX = cx + cos(angle) * (radius * 0.62f)
        val coilY = cy + sin(angle) * (radius * 0.62f)
        drawCircle(
            color = nodeColor,
            radius = 2.2f,
            center = Offset(coilX, coilY)
        )
    }

    // Rotor central
    drawCircle(
        color = if (mode == EnergyMode.STANDBY) Color(0xFF334155) else Color.White,
        radius = 4.5f,
        center = Offset(cx, cy)
    )
}

/**
 * Desenha o nó da Bateria de Alta Tensão HV (sob os bancos traseiros / porta-malas).
 */
private fun DrawScope.drawHighVoltageBatteryNode(
    cx: Float,
    cy: Float,
    soc: Int,
    mode: EnergyMode,
    pulse: Float
) {
    val nodeWidth = 92.dp.toPx()
    val nodeHeight = 36.dp.toPx()
    val cornerRadius = CornerRadius(8f, 8f)

    val socColor = getSocColor(soc)
    val glowColor = if (mode == EnergyMode.CHARGING) Color(0xFF00FF66) else socColor

    // Halo brilhante quando carregando
    if (mode == EnergyMode.CHARGING) {
        drawRoundRect(
            color = glowColor.copy(alpha = 0.25f * pulse),
            topLeft = Offset(cx - nodeWidth * 0.55f, cy - nodeHeight * 0.55f),
            size = Size(nodeWidth * 1.10f, nodeHeight * 1.10f),
            cornerRadius = CornerRadius(12f, 12f)
        )
    }

    // Corpo da bateria
    drawRoundRect(
        color = Color(0xFF091320),
        topLeft = Offset(cx - nodeWidth / 2f, cy - nodeHeight / 2f),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        color = glowColor.copy(alpha = 0.85f),
        topLeft = Offset(cx - nodeWidth / 2f, cy - nodeHeight / 2f),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = if (mode == EnergyMode.CHARGING) 2.5f else 1.5f)
    )

    // Células internas de íon-lítio/NiMH
    val cellCount = 6
    val cellMargin = 4f
    val totalInnerW = nodeWidth - 16f
    val cellW = (totalInnerW - cellMargin * (cellCount - 1)) / cellCount
    val cellH = nodeHeight * 0.45f
    val startX = cx - nodeWidth / 2f + 8f
    val innerY = cy - cellH / 2f

    val filledCells = (soc / 100f * cellCount).roundToInt().coerceIn(1, cellCount)

    for (i in 0 until cellCount) {
        val x = startX + i * (cellW + cellMargin)
        val isFilled = i < filledCells
        drawRoundRect(
            color = if (isFilled) socColor else Color(0xFF1E293B),
            topLeft = Offset(x, innerY),
            size = Size(cellW, cellH),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
    }
}

/**
 * Desenha as rodas com pneus pretos esportivos e cubos com luzes giratórias.
 */
private fun DrawScope.drawWheelNodes(
    leftX: Float,
    rightX: Float,
    y: Float,
    speed: Float,
    isFront: Boolean,
    isDriven: Boolean
) {
    val wheelW = 14.dp.toPx()
    val wheelH = 34.dp.toPx()

    listOf(leftX, rightX).forEach { wx ->
        // Pneu de borracha preta
        drawRoundRect(
            color = Color(0xFF111827),
            topLeft = Offset(wx - wheelW / 2f, y - wheelH / 2f),
            size = Size(wheelW, wheelH),
            cornerRadius = CornerRadius(5f, 5f)
        )
        // Roda esportiva cinza grafite
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(wx - wheelW * 0.35f, y - wheelH * 0.35f),
            size = Size(wheelW * 0.70f, wheelH * 0.70f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Cubo de roda / freio esportivo iluminado
        val hubColor = if (isDriven && speed > 2f) Color(0xFF00FFCC) else Color(0xFF64748B)
        drawCircle(
            color = hubColor,
            radius = 3.5f,
            center = Offset(wx, y)
        )
    }
}

// ============================================================================
// CARDS INFORMATIVOS DA BATERIA HV E TREM DE FORÇA
// ============================================================================
@Composable
private fun HvBatterySocCard(telemetry: TelemetryState) {
    val soc = telemetry.hybridBatterySoc
    val socColor = getSocColor(soc)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = socColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BATERIA DE ALTA TENSÃO (HV)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (soc > 0) "$soc%" else "Standby",
                    color = socColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra progressiva de SOC segmentada por cor
            LinearProgressIndicator(
                progress = { (soc / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = socColor,
                trackColor = Color(0xFF162030)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        soc < 20 -> "Carga baixa (Recarga prioritária)"
                        soc <= 50 -> "Carga intermediária"
                        else -> "Carga ideal / Pronta"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Text(
                    text = if (telemetry.isHybridDetected) "ECU 7E2 Conectada" else "Monitor THS",
                    color = Color(0xFF00FFCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PowertrainStatsCard(
    telemetry: TelemetryState,
    effectiveKw: Float,
    mode: EnergyMode
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, CockpitCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "POTÊNCIA INSTANTÂNEA & TELEMETRIA",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                val kwColor = when {
                    effectiveKw < -1f -> Color(0xFF00FF66) // Carga / regeneração
                    effectiveKw > 1f -> Color(0xFF00F0FF) // Descarga
                    else -> Color(0xFF94A3B8)
                }

                Text(
                    text = String.format("%+.1f kW", effectiveKw),
                    color = kwColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Barra gráfica bi-direcional de potência: Recarga (-) à esquerda, Descarga (+) à direita
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RECARGA (-)", color = Color(0xFF00FF66), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("0 kW", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("DESCARGA (+)", color = Color(0xFF00F0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                BiDirectionalPowerBar(effectiveKw)
            }

            // Grid com 4 parâmetros fundamentais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "VELOCIDADE",
                    value = "${telemetry.speed.toInt()}",
                    unit = "km/h",
                    color = Color(0xFF00E5FF)
                )
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "MOTOR TÉRMICO",
                    value = if (telemetry.rpm > 0) "${telemetry.rpm}" else "OFF",
                    unit = if (telemetry.rpm > 0) "RPM" else "",
                    color = if (telemetry.rpm > 0) Color(0xFFFF9100) else Color(0xFF64748B)
                )
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "CARGA MOTOR",
                    value = "${telemetry.engineLoad}%",
                    unit = "",
                    color = if (telemetry.engineLoad > 60) StatusAmber else Color(0xFF00FFCC)
                )
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "CONSUMO INST.",
                    value = if (telemetry.instantConsumption > 0f) String.format("%.1f", telemetry.instantConsumption) else if (mode == EnergyMode.EV) "EV" else "—",
                    unit = if (telemetry.instantConsumption > 0f) "km/L" else "",
                    color = Color(0xFF00FF66)
                )
            }
        }
    }
}

@Composable
private fun BiDirectionalPowerBar(powerKw: Float) {
    val maxKw = 50f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        val w = size.width
        val h = size.height
        val midX = w / 2f

        // Fundo
        drawRect(Color(0xFF162030))

        // Linha central divisória
        drawLine(
            color = Color(0xFF475569),
            start = Offset(midX, 0f),
            end = Offset(midX, h),
            strokeWidth = 2f
        )

        if (powerKw < 0f) {
            // Recarga / Regeneração (Preenchimento à esquerda do centro em Verde)
            val fraction = (-powerKw / maxKw).coerceIn(0f, 1f)
            val barW = fraction * midX
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00FF66), Color(0xFF00FFCC)),
                    startX = midX - barW,
                    endX = midX
                ),
                topLeft = Offset(midX - barW, 0f),
                size = Size(barW, h)
            )
        } else if (powerKw > 0f) {
            // Descarga / Tração (Preenchimento à direita do centro em Ciano)
            val fraction = (powerKw / maxKw).coerceIn(0f, 1f)
            val barW = fraction * midX
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0083FF)),
                    startX = midX,
                    endX = midX + barW
                ),
                topLeft = Offset(midX, 0f),
                size = Size(barW, h)
            )
        }
    }
}

@Composable
private fun MetricMiniTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0E1624),
        border = BorderStroke(1.dp, Color(0xFF1B2A40))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color(0xFF64748B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = unit, color = Color(0xFF94A3B8), fontSize = 9.sp, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenActionButtons(
    isDemo: Boolean,
    isPowerInverted: Boolean,
    onToggleDemo: () -> Unit,
    onTogglePowerInvert: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onToggleDemo,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDemo) StatusAmber else Color(0xFF00FFCC).copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, if (isDemo) StatusAmber else Color(0xFF00FFCC))
        ) {
            Icon(
                if (isDemo) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isDemo) CockpitBlack else Color(0xFF00FFCC),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isDemo) "Parar Simulador" else "Simulador Demo",
                color = if (isDemo) CockpitBlack else Color(0xFF00FFCC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onTogglePowerInvert,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, Color(0xFF263248))
        ) {
            Icon(
                Icons.Default.SyncAlt,
                contentDescription = null,
                tint = if (isPowerInverted) Color(0xFF00FFCC) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPowerInverted) "Sinal kW: Inv" else "Sinal kW: Normal",
                color = TextPrimary,
                fontSize = 11.5.sp
            )
        }
    }
}

// ============================================================================
// AUXILIARES MATEMÁTICOS DE ANIMAÇÃO
// ============================================================================
private fun calculateFlowDuration(mode: EnergyMode, powerKw: Float, speed: Float): Int {
    if (mode == EnergyMode.STANDBY) return 2000
    val intensity = abs(powerKw).coerceIn(1f, 50f)
    // Mais potência (kW) ou velocidade faz as partículas fluírem mais rápido (menor tempo de ciclo)
    val base = 1600 - (intensity * 22).toInt() - (speed * 2f).toInt()
    return base.coerceIn(380, 1600)
}

private fun calculateRoadDuration(speed: Float): Int {
    if (speed < 1f) return 3000
    // Quanto maior a velocidade do carro, mais rápido passam as listras da pista
    val base = 2500 - (speed * 18).toInt()
    return base.coerceIn(250, 2500)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

// ============================================================================
// PREVIEWS COM CADA UM DOS 5 MODOS DE ENERGIA EXIGIDOS
// ============================================================================
@Preview(name = "1. STANDBY", showBackground = true)
@Composable
fun PreviewEnergyFlowStandby() {
    MyApplicationTheme {
        EnergyFlowScreen(
            telemetry = TelemetryState(
                speed = 0f,
                rpm = 0,
                hybridBatterySoc = 62,
                hybridMotorPowerKw = 0f,
                isHybridDetected = true,
                rangeKm = 720,
                tripDurationSec = 120,
                tripAvgSpeed = 0f
            )
        )
    }
}

@Preview(name = "2. MODO EV", showBackground = true)
@Composable
fun PreviewEnergyFlowEv() {
    MyApplicationTheme {
        EnergyFlowScreen(
            telemetry = TelemetryState(
                speed = 36f,
                rpm = 0,
                hybridBatterySoc = 61,
                hybridMotorPowerKw = 16.5f,
                isHybridDetected = true,
                rangeKm = 720,
                tripDurationSec = 340,
                tripAvgSpeed = 32.5f
            )
        )
    }
}

@Preview(name = "3. MODO COMBUSTÃO", showBackground = true)
@Composable
fun PreviewEnergyFlowEngine() {
    MyApplicationTheme {
        EnergyFlowScreen(
            telemetry = TelemetryState(
                speed = 88f,
                rpm = 1850,
                hybridBatterySoc = 60,
                hybridMotorPowerKw = 0.5f,
                isHybridDetected = true,
                rangeKm = 710,
                tripDurationSec = 780,
                tripAvgSpeed = 58.2f
            )
        )
    }
}

@Preview(name = "4. MODO HÍBRIDO", showBackground = true)
@Composable
fun PreviewEnergyFlowHybrid() {
    MyApplicationTheme {
        EnergyFlowScreen(
            telemetry = TelemetryState(
                speed = 92f,
                rpm = 3400,
                engineLoad = 78,
                hybridBatterySoc = 59,
                hybridMotorPowerKw = 34.0f,
                isHybridDetected = true,
                rangeKm = 695,
                tripDurationSec = 920,
                tripAvgSpeed = 61.4f
            )
        )
    }
}

@Preview(name = "5. MODO CARREGANDO / REGENERAÇÃO", showBackground = true)
@Composable
fun PreviewEnergyFlowCharging() {
    MyApplicationTheme {
        EnergyFlowScreen(
            telemetry = TelemetryState(
                speed = 48f,
                rpm = 0,
                hybridBatterySoc = 64,
                hybridMotorPowerKw = -28.0f,
                isHybridDetected = true,
                rangeKm = 715,
                tripDurationSec = 1150,
                tripAvgSpeed = 59.8f
            )
        )
    }
}
