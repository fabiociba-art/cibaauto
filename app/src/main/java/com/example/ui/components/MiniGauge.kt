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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MiniGaugeCenterType {
    ICON_OIL,
    ICON_COOLANT,
    ICON_ETHANOL,
    NEEDLE_TURBO,
    NEEDLE_BATTERY,
    NEEDLE_GFORCE,
    NEEDLE_SOC // nível de carga: baixo = vermelho, alto = verde
}

/** Etanol na gasolina: quanto maior a concentração, mais vermelho (verde -> amarelo -> vermelho). */
private fun ethanolColorFor(percent: Float): Color {
    val f = (percent / 100f).coerceIn(0f, 1f)
    return if (f < 0.5f) lerp(GaugeGreen, GaugeYellow, f / 0.5f)
    else lerp(GaugeYellow, GaugeRed, (f - 0.5f) / 0.5f)
}

/** Cores invertidas (nível de carga): baixo = vermelho, médio = amarelo, alto = verde. */
private fun invertedColorFor(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return if (f < 0.5f) lerp(GaugeRed, GaugeYellow, f / 0.5f)
    else lerp(GaugeYellow, GaugeGreen, (f - 0.5f) / 0.5f)
}

/** Bateria 12 V: abaixo de ~11,8 V vermelho, até ~12,4 V amarelo, acima verde. */
private fun battery12ColorFor(volts: Float): Color = when {
    volts < 11.8f -> GaugeRed
    volts < 12.4f -> GaugeYellow
    else -> GaugeGreen
}

@Composable
fun MiniGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    label: String,
    unit: String = "",
    centerType: MiniGaugeCenterType = MiniGaugeCenterType.NEEDLE_TURBO,
    displayValue: String? = null,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        // NaN derruba o Compose (AnimationVector cannot contain a NaN): sem leitura = mínimo
        targetValue = if (value.isNaN()) minValue else value.coerceIn(minValue, maxValue),
        animationSpec = tween(durationMillis = 150),
        label = "mini_gauge_$label"
    )

    val fraction = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.8.dp.toPx()
                val diameter = this.size.minDimension - strokeWidth * 2
                val arcSize = Size(diameter, diameter)
                val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = diameter / 2f

                val startAngle = 135f
                val sweepAngle = 270f

                // 1. Arc background track
                drawArc(
                    color = Color(0xFF1E222A),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Multi-color arc: customized for ethanol thresholds (0-30% Green, 31-65% Yellow, 66-100% Red) or general sweep
                val currentSweep = sweepAngle * fraction
                val safeValue = if (value.isNaN()) minValue else value
                val arcBrush: Brush = when (centerType) {
                    // Etanol: quanto maior a concentração, mais vermelho
                    MiniGaugeCenterType.ICON_ETHANOL ->
                        androidx.compose.ui.graphics.SolidColor(ethanolColorFor(safeValue))
                    // SOC híbrida: cores invertidas (baixo vermelho, alto verde)
                    MiniGaugeCenterType.NEEDLE_SOC ->
                        androidx.compose.ui.graphics.SolidColor(invertedColorFor(fraction))
                    // Bateria 12 V: cores invertidas (tensão baixa vermelho, normal verde)
                    MiniGaugeCenterType.NEEDLE_BATTERY ->
                        androidx.compose.ui.graphics.SolidColor(battery12ColorFor(safeValue))
                    else -> Brush.sweepGradient(
                        0.0f to GaugeCyan,
                        0.45f to GaugeGreen,
                        0.70f to GaugeYellow,
                        0.90f to GaugeRed,
                        1.0f to GaugeRed,
                        center = center
                    )
                }

                if (currentSweep > 1f) {
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

                // 3. Inner Dial Disc with 3D bezel border
                val innerRadius = radius - strokeWidth - 2.dp.toPx()
                drawCircle(
                    color = Color(0xFF151820),
                    radius = innerRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF2C3240),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // 4. Tick markings on dial
                val numTicks = 8
                for (i in 0..numTicks) {
                    val tickAngle = startAngle + (i.toFloat() / numTicks) * sweepAngle
                    val tickRad = (tickAngle * PI / 180.0).toFloat()
                    val tStart = innerRadius - 1.dp.toPx()
                    val tEnd = innerRadius - 5.dp.toPx()

                    drawLine(
                        color = Color(0xFF7B8394),
                        start = Offset(center.x + tStart * cos(tickRad), center.y + tStart * sin(tickRad)),
                        end = Offset(center.x + tEnd * cos(tickRad), center.y + tEnd * sin(tickRad)),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 5. Center Element: Icon or Needle
                when (centerType) {
                    MiniGaugeCenterType.ICON_OIL -> {
                        // Draw Oil Lamp silhouette in red/amber
                        val oilPath = Path().apply {
                            val ox = center.x
                            val oy = center.y + 2.dp.toPx()
                            moveTo(ox - 10.dp.toPx(), oy)
                            lineTo(ox + 6.dp.toPx(), oy)
                            lineTo(ox + 9.dp.toPx(), oy - 4.dp.toPx())
                            lineTo(ox + 12.dp.toPx(), oy - 3.dp.toPx())
                            lineTo(ox + 7.dp.toPx(), oy + 3.5.dp.toPx())
                            lineTo(ox - 9.dp.toPx(), oy + 3.5.dp.toPx())
                            close()
                        }
                        drawPath(oilPath, color = GaugeRed)
                        drawCircle(
                            color = GaugeRed,
                            radius = 2.dp.toPx(),
                            center = Offset(center.x + 13.dp.toPx(), center.y - 1.dp.toPx())
                        )
                    }

                    MiniGaugeCenterType.ICON_COOLANT -> {
                        // Draw Coolant Thermometer silhouette in cyan/blue
                        val cx = center.x
                        val cy = center.y
                        drawCircle(
                            color = GaugeCyan,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, cy + 5.dp.toPx())
                        )
                        drawLine(
                            color = GaugeCyan,
                            start = Offset(cx, cy + 5.dp.toPx()),
                            end = Offset(cx, cy - 7.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = GaugeCyan,
                            start = Offset(cx - 8.dp.toPx(), cy + 11.dp.toPx()),
                            end = Offset(cx + 8.dp.toPx(), cy + 11.dp.toPx()),
                            strokeWidth = 1.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    MiniGaugeCenterType.ICON_ETHANOL -> {
                        // Draw Ethanol fuel drop & eco leaf silhouette in vibrant green/cyan
                        val cx = center.x
                        val cy = center.y - 3.dp.toPx()
                        val dropPath = Path().apply {
                            moveTo(cx, cy - 8.dp.toPx())
                            cubicTo(
                                cx - 6.dp.toPx(), cy - 2.dp.toPx(),
                                cx - 7.dp.toPx(), cy + 5.dp.toPx(),
                                cx, cy + 8.dp.toPx()
                            )
                            cubicTo(
                                cx + 7.dp.toPx(), cy + 5.dp.toPx(),
                                cx + 6.dp.toPx(), cy - 2.dp.toPx(),
                                cx, cy - 8.dp.toPx()
                            )
                            close()
                        }
                        drawPath(dropPath, color = ethanolColorFor(if (value.isNaN()) minValue else value))
                    }

                    MiniGaugeCenterType.NEEDLE_TURBO,
                    MiniGaugeCenterType.NEEDLE_BATTERY,
                    MiniGaugeCenterType.NEEDLE_GFORCE,
                    MiniGaugeCenterType.NEEDLE_SOC -> {
                        // Center dark metallic knob
                        drawCircle(
                            color = Color(0xFF222630),
                            radius = 13.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF3B4254),
                            radius = 13.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF14171E),
                            radius = 7.5.dp.toPx(),
                            center = center
                        )

                        // Needle pointer
                        val needleAngle = startAngle + fraction * sweepAngle
                        val needleRad = (needleAngle * PI / 180.0).toFloat()
                        val needleLen = innerRadius - 6.dp.toPx()

                        val needleColor = if (centerType == MiniGaugeCenterType.NEEDLE_SOC) {
                            invertedColorFor(fraction)
                        } else if (centerType == MiniGaugeCenterType.NEEDLE_BATTERY) {
                            battery12ColorFor(safeValue)
                        } else when {
                            fraction <= 0.45f -> GaugeGreen
                            fraction <= 0.75f -> GaugeYellow
                            else -> GaugeRed
                        }

                        val tip = Offset(center.x + needleLen * cos(needleRad), center.y + needleLen * sin(needleRad))
                        drawLine(
                            color = needleColor,
                            start = center,
                            end = tip,
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // High contrast display value in center bottom of dial
            if (displayValue != null) {
                val displayColor = if (centerType == MiniGaugeCenterType.ICON_ETHANOL && displayValue != "--") {
                    ethanolColorFor(if (value.isNaN()) minValue else value)
                } else {
                    TextWhite
                }
                Text(
                    text = displayValue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = displayColor,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Title below gauge (larger and bolder)
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
    }
}
