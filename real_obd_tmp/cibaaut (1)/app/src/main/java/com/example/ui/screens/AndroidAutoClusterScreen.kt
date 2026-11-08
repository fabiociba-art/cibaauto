package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Tela de cluster digital (Android Auto Cluster) rigorosamente baseada
 * no layout de cockpit da referência visual fornecida pelo usuário.
 */
@Composable
fun AndroidAutoClusterScreen(telemetry: TelemetryState) {
    val speed = telemetry.speed.coerceIn(0f, 200f)
    val rpm = telemetry.rpm.coerceIn(0, 8000)
    val battery = telemetry.batteryVoltage
    val ethanol = telemetry.ethanolPercentage
    val rangeKm = if (telemetry.averageConsumption > 0) {
        ((50f - 0f) * telemetry.averageConsumption).toInt().coerceIn(100, 900)
    } else 650

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
    ) {
        // Fundo com gradiente radial sutil de cockpit
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0A111C), Color(0xFF030508)),
                        radius = 1100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP HEADER: Logo & Title ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // Ícone de asas estilizado
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ONBOARD COMPUTER",
                    color = Color(0xFF8BA3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Linha divisória com ponto central
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.width(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0xFF00FFCC))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color(0xFF00FFCC), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00FFCC), Color.Transparent)
                                )
                            )
                    )
                }
            }

            // ── TOP METRICS CHIPS (Battery & Ethanol) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopMetricCard(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "BATTERY",
                    value = String.format("%.1fV", battery),
                    accentColor = Color(0xFF00E5FF)
                )
                TopMetricCard(
                    icon = Icons.Default.LocalGasStation,
                    label = "ETHANOL",
                    value = "$ethanol%",
                    accentColor = Color(0xFF00FF66)
                )
            }

            // ── MAIN GAUGES ROW (Speedometer & Tachometer side-by-side) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: Speedometer (Cyan Neon)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdvancedCircularGauge(
                        value = speed,
                        maxValue = 200f,
                        progressColor = Color(0xFF00E5FF),
                        trackColor = Color(0xFF101B2B),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        isSpeedometer = true
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SPEED",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = speed.toInt().toString(),
                            color = Color.White,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 16f
                                )
                            )
                        )
                        Text(
                            text = "KM/H",
                            color = Color(0xFF718096),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // RIGHT: Tachometer (Green Neon)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AdvancedCircularGauge(
                        value = rpm.toFloat(),
                        maxValue = 8000f,
                        progressColor = Color(0xFF00FF66),
                        trackColor = Color(0xFF101B2B),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        isSpeedometer = false
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RPM",
                            color = Color(0xFF00FF66),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rpm.toString(),
                            color = Color.White,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF00FF66).copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 16f
                                )
                            )
                        )
                        Text(
                            text = "RPM",
                            color = Color(0xFF718096),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // ── INDICATOR LIGHTS STRIP ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF080D16), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF162235), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Highlight, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.WbTwilight, contentDescription = null, tint = Color(0xFF4A5568), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.AirlineSeatIndividualSuite, contentDescription = null, tint = Color(0xFF718096), modifier = Modifier.size(20.dp))
                
                Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0xFF1E293B)))

                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Highlight, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── BOTTOM CARDS ROW (Odometer & Range) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Odometer Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF090F19), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF1B2A40), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "ODOMETER",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "12,450 KM",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Range Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF090F19), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF1B2A40), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF00FF66).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "RANGE",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$rangeKm KM",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── PAGINATION DOTS ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E5FF), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF334155), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF334155), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF334155), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF334155), CircleShape))
            }
        }
    }
}

@Composable
private fun TopMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF090F19), RoundedCornerShape(22.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdvancedCircularGauge(
    value: Float,
    maxValue: Float,
    progressColor: Color,
    trackColor: Color,
    startAngle: Float = 135f,
    sweepAngle: Float = 270f,
    isSpeedometer: Boolean
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 14.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        // Track Arc
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress Arc (Neon)
        val progressSweep = sweepAngle * progress
        drawArc(
            color = progressColor,
            startAngle = startAngle,
            sweepAngle = progressSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Tick marks & numbers representation
        val tickCount = if (isSpeedometer) 10 else 8
        for (i in 0..tickCount) {
            val t = i / tickCount.toFloat()
            val angleDeg = startAngle + sweepAngle * t
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val outerR = diameter / 2f
            val innerR = outerR - strokeWidth * 0.7f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val x1 = cx + cos(angleRad).toFloat() * innerR
            val y1 = cy + sin(angleRad).toFloat() * innerR
            val x2 = cx + cos(angleRad).toFloat() * outerR
            val y2 = cy + sin(angleRad).toFloat() * outerR

            drawLine(
                color = Color(0xFF334155),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2.5f
            )
        }
    }
}
