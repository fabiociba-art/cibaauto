package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DriveMode
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.isActive

@Composable
fun AnimatedEnergyRoad(
    driveMode: DriveMode,
    speedKmh: Float,
    accelPercent: Float = 0f,
    isPedal: Boolean = false,
    ecuLinkOk: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Flow phase driven dynamically by vehicle speed (slow to fast)
    var flowPhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(speedKmh, ecuLinkOk) {
        if (!ecuLinkOk) return@LaunchedEffect
        var lastTimeNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = frameTimeNanos

                // Speed multiplier: 0 km/h = slow idle (0.06), scales smoothly up to 3.2 at high speed
                val speedFactor = if (speedKmh <= 1.0f) {
                    0.06f
                } else {
                    (0.20f + (speedKmh / 65f) * 0.95f).coerceIn(0.20f, 3.2f)
                }

                flowPhase = (flowPhase + dt * speedFactor) % 1.0f
            }
        }
    }

    // Cor da pista, do veículo e rotulação orientadas pelo acelerador (pedal real ou borboleta)
    val neutralColor = Color(0xFF455A64)
    val (baseColor, carDrawable, modeTag, indicatorText) = if (!ecuLinkOk) {
        Quadruple(
            neutralColor,
            R.drawable.car_intact_clean,
            "--",
            if (isPedal) "Pedal --" else "Acelerador --"
        )
    } else {
        val (color, drawable, tag) = when {
            accelPercent <= 25f -> Triple(GaugeGreen, R.drawable.car_intact_green, "ECO")
            accelPercent <= 60f -> Triple(GaugeYellow, R.drawable.car_intact_yellow, "NORMAL")
            else -> Triple(GaugeRed, R.drawable.car_intact_red, "POWER")
        }
        val label = if (isPedal) "Pedal ${accelPercent.toInt()}%" else "Acelerador ${accelPercent.toInt()}%"
        Quadruple(color, drawable, tag, label)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // 1. Dynamic Canvas: Road surface, glow borders, dot matrix, and animated chevrons
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEnergyRoadWithChevrons(
                baseColor = baseColor,
                flowPhase = flowPhase,
                drawChevrons = ecuLinkOk
            )
        }

        // 2. Toyota Corolla Sedan 100% completo, sem cortes
        Image(
            painter = painterResource(id = carDrawable),
            contentDescription = "Veículo na faixa de energia ($indicatorText)",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = (-20).dp)
                .size(width = 255.dp, height = 114.dp),
            contentScale = ContentScale.Fit
        )

        // 3. Indicador do modo do acelerador à direita
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = modeTag,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = baseColor,
                letterSpacing = 1.sp
            )
            Text(
                text = indicatorText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun DrawScope.drawEnergyRoadWithChevrons(
    baseColor: Color,
    flowPhase: Float,
    drawChevrons: Boolean
) {
    val w = size.width
    val h = size.height

    // Trajetória da pista de ponta a ponta
    val topStart = Offset(0f, h * 0.72f)
    val topEnd = Offset(w, h * 0.22f)
    val bottomStart = Offset(0f, h * 0.98f)
    val bottomEnd = Offset(w, h * 0.48f)

    val roadPath = Path().apply {
        moveTo(topStart.x, topStart.y)
        lineTo(topEnd.x, topEnd.y)
        lineTo(bottomEnd.x, bottomEnd.y)
        lineTo(bottomStart.x, bottomStart.y)
        close()
    }

    // 1. Superfície da estrada com degradê
    val roadBrush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.92f),
            baseColor.copy(alpha = 0.78f),
            baseColor.copy(alpha = 0.62f)
        ),
        start = Offset(0f, h * 0.85f),
        end = Offset(w, h * 0.35f)
    )
    drawPath(path = roadPath, brush = roadBrush)

    // 2. Bordas luminosas
    drawLine(
        color = Color.White.copy(alpha = if (drawChevrons) 0.88f else 0.4f),
        start = topStart,
        end = topEnd,
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = baseColor.copy(alpha = 0.92f),
        start = bottomStart,
        end = bottomEnd,
        strokeWidth = 2.dp.toPx()
    )

    // 3. Matriz de pontos de textura
    val roadDir = (topEnd - topStart)
    val roadLen = roadDir.getDistance()
    val unitX = roadDir.x / roadLen
    val unitY = roadDir.y / roadLen

    val perpX = -unitY
    val perpY = unitX
    val roadWidthPx = (bottomStart.y - topStart.y) * 0.84f

    val numDotsAlong = (roadLen / 14f).toInt()
    for (i in 0..numDotsAlong) {
        val tAlong = i.toFloat() / numDotsAlong
        val baseX = topStart.x + roadDir.x * tAlong
        val baseY = topStart.y + roadDir.y * tAlong

        for (row in 1..4) {
            val tAcross = row / 5f
            val dotX = baseX + perpX * (roadWidthPx * tAcross)
            val dotY = baseY + perpY * (roadWidthPx * tAcross)

            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = 1.3.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }
    }

    if (!drawChevrons) return

    // 4. Chevrons luminosos animados
    val numChevrons = 5
    val chevronSpacing = 1f / numChevrons

    for (c in 0..numChevrons) {
        val chevronPos = ((c * chevronSpacing + flowPhase) % 1.0f)
        if (chevronPos !in 0.05f..0.96f) continue

        val apexCenterAlong = chevronPos * roadLen
        val apexX = topStart.x + unitX * apexCenterAlong + perpX * (roadWidthPx * 0.5f)
        val apexY = topStart.y + unitY * apexCenterAlong + perpY * (roadWidthPx * 0.5f)

        val alphaFade = when {
            chevronPos < 0.20f -> chevronPos / 0.20f
            chevronPos > 0.85f -> (1.0f - chevronPos) / 0.15f
            else -> 1.0f
        }.coerceIn(0f, 1f)

        val chevronDepth = 22.dp.toPx()
        val chevronWingSpan = roadWidthPx * 0.38f
        val wingSteps = 6

        for (step in 0..wingSteps) {
            val wingFraction = step.toFloat() / wingSteps
            val backX = -unitX * (wingFraction * chevronDepth)
            val backY = -unitY * (wingFraction * chevronDepth)

            val upperX = apexX + backX - perpX * (wingFraction * chevronWingSpan)
            val upperY = apexY + backY - perpY * (wingFraction * chevronWingSpan)

            val lowerX = apexX + backX + perpX * (wingFraction * chevronWingSpan)
            val lowerY = apexY + backY + perpY * (wingFraction * chevronWingSpan)

            val dotAlpha = (0.95f - wingFraction * 0.35f) * alphaFade
            val dotRadius = (2.6f - wingFraction * 0.8f).dp.toPx()

            // Ponto brilhante
            drawCircle(
                color = Color.White.copy(alpha = dotAlpha),
                radius = dotRadius,
                center = Offset(upperX, upperY)
            )
            drawCircle(
                color = baseColor.copy(alpha = dotAlpha * 0.4f),
                radius = dotRadius * 1.8f,
                center = Offset(upperX, upperY)
            )

            if (step > 0) {
                drawCircle(
                    color = Color.White.copy(alpha = dotAlpha),
                    radius = dotRadius,
                    center = Offset(lowerX, lowerY)
                )
                drawCircle(
                    color = baseColor.copy(alpha = dotAlpha * 0.4f),
                    radius = dotRadius * 1.8f,
                    center = Offset(lowerX, lowerY)
                )
            }
        }
    }
}
