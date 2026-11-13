package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeed: Float,
    avgSpeed: Float = 0f,
    maxSpeed: Float = 220f,
    size: Dp = 150.dp,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = if (currentSpeed.isNaN()) 0f else currentSpeed.coerceIn(0f, maxSpeed),
        animationSpec = tween(durationMillis = 180),
        label = "speed"
    )

    val isCompact = size <= 170.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (isCompact) 9.dp.toPx() else 14.dp.toPx()
            val innerStrokeWidth = if (isCompact) 2.dp.toPx() else 3.dp.toPx()
            val diameter = this.size.minDimension - strokeWidth * 2
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = diameter / 2f

            val startAngle = 135f
            val sweepAngle = 270f

            // 1. Background Track Arc
            drawArc(
                color = Color(0xFF1E222B),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Multi-color Gradient Active Arc (Green -> Yellow -> Red)
            val speedFraction = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)
            val currentSweep = sweepAngle * speedFraction

            val arcBrush = Brush.sweepGradient(
                0.0f to GaugeGreen,
                0.45f to GaugeGreen,
                0.65f to GaugeYellow,
                0.88f to GaugeRed,
                1.0f to GaugeRed,
                center = center
            )

            if (currentSweep > 0.5f) {
                drawArc(
                    brush = arcBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Inner decorative thin ring
            val ringOffset = if (isCompact) 14.dp.toPx() else 24.dp.toPx()
            val innerDiameter = diameter - ringOffset
            val innerTopLeft = Offset((this.size.width - innerDiameter) / 2f, (this.size.height - innerDiameter) / 2f)
            val innerSize = Size(innerDiameter, innerDiameter)

            drawArc(
                color = Color(0xFF2A303C),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Round)
            )

            // 4. Dial Tick marks
            val totalTicks = 18
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val angle = startAngle + tickFraction * sweepAngle
                val rad = (angle * PI / 180.0).toFloat()

                val tickLen = if (i % 2 == 0) (if (isCompact) 6.dp.toPx() else 10.dp.toPx()) else (if (isCompact) 3.dp.toPx() else 5.dp.toPx())
                val tickColor = when {
                    tickFraction <= 0.23f -> GaugeGreen.copy(alpha = 0.85f)
                    tickFraction <= 0.45f -> GaugeYellow.copy(alpha = 0.85f)
                    else -> GaugeRed.copy(alpha = 0.85f)
                }

                val startRadius = radius - strokeWidth / 2f - 2.dp.toPx()
                val endRadius = startRadius - tickLen

                val p1 = Offset(center.x + startRadius * cos(rad), center.y + startRadius * sin(rad))
                val p2 = Offset(center.x + endRadius * cos(rad), center.y + endRadius * sin(rad))

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (i % 2 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Needle Indicator
            val needleAngle = startAngle + speedFraction * sweepAngle
            val needleRad = (needleAngle * PI / 180.0).toFloat()
            val needleLen = radius - (if (isCompact) 10.dp.toPx() else 16.dp.toPx())

            val needleColor = when {
                animatedSpeed <= 50f -> GaugeGreen
                animatedSpeed <= 90f -> GaugeYellow
                else -> GaugeRed
            }

            drawCircle(
                color = needleColor.copy(alpha = 0.4f),
                radius = if (isCompact) 10.dp.toPx() else 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF14171E),
                radius = if (isCompact) 7.dp.toPx() else 11.dp.toPx(),
                center = center
            )
            drawCircle(
                color = needleColor,
                radius = if (isCompact) 2.5.dp.toPx() else 4.dp.toPx(),
                center = center
            )

            val needleTip = Offset(center.x + needleLen * cos(needleRad), center.y + needleLen * sin(needleRad))
            drawLine(
                color = needleColor,
                start = center,
                end = needleTip,
                strokeWidth = if (isCompact) 2.4.dp.toPx() else 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Inner Numbers and Labels - Clean and Centered
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animatedSpeed.toInt()}",
                fontSize = if (isCompact) 34.sp else 46.sp,
                fontWeight = FontWeight.Black,
                color = TextWhite,
                lineHeight = if (isCompact) 36.sp else 48.sp
            )
            Text(
                text = "km/h",
                fontSize = if (isCompact) 11.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = "Velocímetro",
                fontSize = if (isCompact) 9.sp else 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted
            )
        }
    }
}
