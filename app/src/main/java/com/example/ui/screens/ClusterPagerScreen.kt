package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.ui.theme.DynamicGaugeColors
import com.example.data.InmetroConsumption
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Tipos de medidores disponíveis para personalização das páginas 2 a 5.
 */
enum class GaugeType(val title: String, val unit: String, val maxValue: Float, val color: Color, val icon: ImageVector) {
    SPEED("VELOCIDADE", "KM/H", 200f, Color(0xFF00E5FF), Icons.Default.Speed),
    RPM("RPM", "RPM", 8000f, Color(0xFF00FF66), Icons.Default.Timer),
    COOLANT("ÁGUA", "°C", 130f, Color(0xFFFF5555), Icons.Default.DeviceThermostat),
    INTAKE("AR IAT", "°C", 80f, Color(0xFF00E5FF), Icons.Default.Thermostat),
    LOAD("CARGA", "%", 100f, Color(0xFFFF9900), Icons.Default.Speed),
    THROTTLE("ACELERADOR", "%", 100f, Color(0xFF00FFCC), Icons.Default.AirlineSeatReclineExtra),
    MAP("PRESSÃO MAP", "KPA", 250f, Color(0xFF9966FF), Icons.Default.Compress),
    BATTERY("BATERIA", "V", 18f, Color(0xFF00E5FF), Icons.Default.BatteryChargingFull),
    ETHANOL("ETANOL", "%", 100f, Color(0xFF00FF66), Icons.Default.LocalGasStation),
    INSTANT_KML("INSTANTÂNEO", "KM/L", 40f, Color(0xFF00FFCC), Icons.Default.Navigation),
    AVG_KML("CONSUMO MÉD", "KM/L", 35f, Color(0xFF00FF66), Icons.Default.Timeline),
    HV_SOC("SOC BATERIA", "%", 100f, Color(0xFF00FF66), Icons.Default.ElectricCar),
    HV_POWER("POTÊNCIA KW", "KW", 60f, Color(0xFF00E5FF), Icons.Default.Bolt),
    FUEL_PRESSURE("PRESSÃO COMB.", "KPA", 500f, Color(0xFFFF9900), Icons.Default.LocalGasStation)
}


private fun dynamicColorFor(type: GaugeType, value: Float): Color {
    return when (type) {
        GaugeType.SPEED -> DynamicGaugeColors.forSpeed(value, type.maxValue)
        GaugeType.RPM -> DynamicGaugeColors.forRpm(value, type.maxValue)
        GaugeType.BATTERY -> DynamicGaugeColors.forBatteryVoltage(value)
        GaugeType.COOLANT -> DynamicGaugeColors.forCoolant(value)
        GaugeType.LOAD -> DynamicGaugeColors.forLoad(value)
        GaugeType.THROTTLE -> DynamicGaugeColors.forThrottle(value)
        GaugeType.INSTANT_KML, GaugeType.AVG_KML -> DynamicGaugeColors.forConsumption(value, InmetroConsumption.forProfile("corolla hybrid", true).referenceGas)
        GaugeType.HV_SOC, GaugeType.ETHANOL -> DynamicGaugeColors.forSoc(value)
        GaugeType.HV_POWER -> DynamicGaugeColors.forGeneric(value, type.maxValue, higherIsBetter = false)
        else -> DynamicGaugeColors.forGeneric(value, type.maxValue)
    }
}

/**
 * Tela do Cluster com Pager deslizante de 5 páginas.
 * Página 1: Cluster original (AndroidAutoClusterScreen)
 * Páginas 2 a 5: Medidores circulares personalizáveis pelo usuário
 */
@Composable
fun ClusterPagerScreen(telemetry: TelemetryState) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ciba_cluster_custom_gauges", Context.MODE_PRIVATE) }
    val pagerState = rememberPagerState(pageCount = { 5 })

    // Estado das configurações de medidores para páginas 1, 2, 3, 4 (índices 1 a 4)
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var customizingPage by remember { mutableStateOf(1) }

    // Carrega medidores das páginas 1..4 de SharedPreferences com valores padrão coerentes
    var pageGauges by remember {
        mutableStateOf(
            listOf(
                // Page 0 não usa (é o cluster original)
                Pair(GaugeType.SPEED, GaugeType.RPM),
                // Page 1 (Página 2)
                Pair(
                    GaugeType.valueOf(prefs.getString("page_1_left", GaugeType.COOLANT.name) ?: GaugeType.COOLANT.name),
                    GaugeType.valueOf(prefs.getString("page_1_right", GaugeType.BATTERY.name) ?: GaugeType.BATTERY.name)
                ),
                // Page 2 (Página 3)
                Pair(
                    GaugeType.valueOf(prefs.getString("page_2_left", GaugeType.LOAD.name) ?: GaugeType.LOAD.name),
                    GaugeType.valueOf(prefs.getString("page_2_right", GaugeType.THROTTLE.name) ?: GaugeType.THROTTLE.name)
                ),
                // Page 3 (Página 4)
                Pair(
                    GaugeType.valueOf(prefs.getString("page_3_left", GaugeType.INSTANT_KML.name) ?: GaugeType.INSTANT_KML.name),
                    GaugeType.valueOf(prefs.getString("page_3_right", GaugeType.AVG_KML.name) ?: GaugeType.AVG_KML.name)
                ),
                // Page 4 (Página 5)
                Pair(
                    GaugeType.valueOf(prefs.getString("page_4_left", GaugeType.HV_SOC.name) ?: GaugeType.HV_SOC.name),
                    GaugeType.valueOf(prefs.getString("page_4_right", GaugeType.HV_POWER.name) ?: GaugeType.HV_POWER.name)
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508))
    ) {
        // Pager com 5 páginas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                // Página 1: Cluster Original
                AndroidAutoClusterScreen(telemetry = telemetry)
            } else {
                // Páginas 2 a 5: Medidores Circulares Personalizáveis
                val (leftGauge, rightGauge) = pageGauges.getOrElse(page) { Pair(GaugeType.SPEED, GaugeType.RPM) }
                CustomClusterPage(
                    pageNumber = page + 1,
                    leftGauge = leftGauge,
                    rightGauge = rightGauge,
                    telemetry = telemetry,
                    onCustomize = {
                        customizingPage = page
                        showCustomizeDialog = true
                    }
                )
            }
        }

        // Rodapé com Pontos Indicadores do Pager (desenhados pelo Pager)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color(0xFF090F19).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 9.dp else 6.dp)
                        .background(
                            if (isSelected) Color(0xFF00FFCC) else Color(0xFF334155),
                            CircleShape
                        )
                )
            }
        }
    }

    // Diálogo de Personalização dos Medidores da Página Atual
    if (showCustomizeDialog) {
        CustomizeGaugesDialog(
            pageNumber = customizingPage + 1,
            currentLeft = pageGauges[customizingPage].first,
            currentRight = pageGauges[customizingPage].second,
            onDismiss = { showCustomizeDialog = false },
            onSave = { newLeft, newRight ->
                val updated = pageGauges.toMutableList()
                updated[customizingPage] = Pair(newLeft, newRight)
                pageGauges = updated
                prefs.edit()
                    .putString("page_${customizingPage}_left", newLeft.name)
                    .putString("page_${customizingPage}_right", newRight.name)
                    .apply()
                showCustomizeDialog = false
            }
        )
    }
}

/**
 * Página Personalizável do Cluster com dois medidores circulares e cartões de apoio.
 */
@Composable
private fun CustomClusterPage(
    pageNumber: Int,
    leftGauge: GaugeType,
    rightGauge: GaugeType,
    telemetry: TelemetryState,
    onCustomize: () -> Unit
) {
    val leftValue = getTelemetryValueForGauge(leftGauge, telemetry)
    val rightValue = getTelemetryValueForGauge(rightGauge, telemetry)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A111C), Color(0xFF030508)),
                    radius = 1100f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header da Página com Botão de Personalização
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PAINEL ADICIONAL • PÁGINA $pageNumber/5",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Medidores Customizados",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCustomize,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF090F19),
                        contentColor = Color(0xFF00FFCC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Personalizar",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Personalizar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Info Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricChip(
                    icon = Icons.Default.Speed,
                    label = "VELOCIDADE",
                    value = "${telemetry.speed.toInt()} KM/H",
                    color = Color(0xFF00E5FF)
                )
                MetricChip(
                    icon = Icons.Default.Timer,
                    label = "RPM",
                    value = "${telemetry.rpm} RPM",
                    color = Color(0xFF00FF66)
                )
            }

            // Área Central com os Dois Medidores Circulares
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Medidor Esquerdo
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularClusterGauge(
                        value = leftValue,
                        maxValue = leftGauge.maxValue,
                        progressColor = dynamicColorFor(leftGauge, leftValue),
                        trackColor = Color(0xFF101B2B),
                        invertColor = leftGauge in listOf(GaugeType.INSTANT_KML, GaugeType.AVG_KML, GaugeType.BATTERY, GaugeType.HV_SOC, GaugeType.ETHANOL)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = leftGauge.title,
                            color = dynamicColorFor(leftGauge, leftValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatGaugeValue(leftGauge, leftValue),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = dynamicColorFor(leftGauge, leftValue).copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 16f
                                )
                            )
                        )
                        Text(
                            text = leftGauge.unit,
                            color = Color(0xFF718096),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Medidor Direito
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularClusterGauge(
                        value = rightValue,
                        maxValue = rightGauge.maxValue,
                        progressColor = dynamicColorFor(rightGauge, rightValue),
                        trackColor = Color(0xFF101B2B),
                        invertColor = rightGauge in listOf(GaugeType.INSTANT_KML, GaugeType.AVG_KML, GaugeType.BATTERY, GaugeType.HV_SOC, GaugeType.ETHANOL)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = rightGauge.title,
                            color = dynamicColorFor(rightGauge, rightValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatGaugeValue(rightGauge, rightValue),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = dynamicColorFor(rightGauge, rightValue).copy(alpha = 0.6f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 16f
                                )
                            )
                        )
                        Text(
                            text = rightGauge.unit,
                            color = Color(0xFF718096),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Rodapé Informativo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090F19), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1B2A40), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(20.dp))
                    Text(text = "Água: ${telemetry.coolantTemp}°C", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                    Text(text = String.format("%.1fV", telemetry.batteryVoltage), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(20.dp))
                    Text(text = "${telemetry.ethanolPercentage}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF090F19), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Column {
            Text(text = label, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun colorForAbsoluteValue(value: Float, max: Float, invert: Boolean = false): Color {
    val green = Color(0xFF00CC55)
    val yellow = Color(0xFFFFAA00)
    val red = Color(0xFFFF3333)
    if (invert) {
        // Consumo, SOC, bateria: alto = verde, baixo = vermelho
        val p = (value / max).coerceIn(0f, 1f)
        return when {
            p >= 0.7f -> green
            p >= 0.4f -> lerp(yellow, green, (p - 0.4f) / 0.3f)
            else -> lerp(red, yellow, (p / 0.4f).coerceIn(0f, 1f))
        }
    }
    return if (max >= 500f) {
        // RPM: 0-1500 verde, 1501-3000 amarelo, 3001+ vermelho
        when {
            value <= 1500f -> green
            value <= 3000f -> lerp(green, yellow, (value - 1500f) / 1500f)
            else -> lerp(yellow, red, ((value - 3000f) / 2000f).coerceIn(0f, 1f))
        }
    } else if (max >= 100f) {
        // Velocidade: 0-50 verde, 51-90 amarelo, 91+ vermelho
        when {
            value <= 50f -> green
            value <= 90f -> lerp(green, yellow, (value - 50f) / 40f)
            else -> lerp(yellow, red, ((value - 90f) / 80f).coerceIn(0f, 1f))
        }
    } else {
        // Percentual: 0-20 verde, 21-50 amarelo, 51+ vermelho
        when {
            value <= 20f -> green
            value <= 50f -> lerp(green, yellow, (value - 20f) / 30f)
            else -> lerp(yellow, red, ((value - 50f) / 50f).coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun CircularClusterGauge(
    value: Float,
    maxValue: Float,
    progressColor: Color,
    trackColor: Color,
    startAngle: Float = 135f,
    sweepAngle: Float = 270f,
    invertColor: Boolean = false
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 18.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Trilha de fundo
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Arco — cor baseada no valor absoluto de cada segmento
        val progressSweep = sweepAngle * progress
        if (progressSweep > 0f) {
            val segments = 28
            val segSweep = progressSweep / segments
            for (i in 0 until segments) {
                val segProgress = (i + 0.5f) / segments * progress
                val absoluteValue = segProgress * maxValue
                val segColor = colorForAbsoluteValue(absoluteValue, maxValue, invertColor)
                drawArc(
                    color = segColor,
                    startAngle = startAngle + i * segSweep,
                    sweepAngle = segSweep + 0.4f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Marcas de escala
        for (i in 0..8) {
            val t = i / 8f
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

private fun getTelemetryValueForGauge(gauge: GaugeType, t: TelemetryState): Float {
    return when (gauge) {
        GaugeType.SPEED -> t.speed
        GaugeType.RPM -> t.rpm.toFloat()
        GaugeType.COOLANT -> t.coolantTemp.toFloat()
        GaugeType.INTAKE -> t.intakeTemp.toFloat()
        GaugeType.LOAD -> t.engineLoad.toFloat()
        GaugeType.THROTTLE -> t.throttlePosition.toFloat()
        GaugeType.MAP -> t.manifoldPressure
        GaugeType.BATTERY -> t.batteryVoltage
        GaugeType.ETHANOL -> t.ethanolPercentage.toFloat()
        GaugeType.INSTANT_KML -> t.instantConsumption
        GaugeType.AVG_KML -> t.averageConsumption
        GaugeType.HV_SOC -> t.hybridBatterySoc.toFloat()
        GaugeType.HV_POWER -> kotlin.math.abs(t.hybridMotorPowerKw)
        GaugeType.FUEL_PRESSURE -> t.fuelPressure
    }
}

private fun formatGaugeValue(gauge: GaugeType, value: Float): String {
    return when (gauge) {
        GaugeType.SPEED, GaugeType.RPM -> value.toInt().toString()
        GaugeType.BATTERY, GaugeType.INSTANT_KML, GaugeType.AVG_KML, GaugeType.HV_POWER -> String.format("%.1f", value)
        else -> value.toInt().toString()
    }
}

/**
 * Diálogo para escolher os dois medidores circulares da página.
 */
@Composable
private fun CustomizeGaugesDialog(
    pageNumber: Int,
    currentLeft: GaugeType,
    currentRight: GaugeType,
    onDismiss: () -> Unit,
    onSave: (GaugeType, GaugeType) -> Unit
) {
    var selectedLeft by remember { mutableStateOf(currentLeft) }
    var selectedRight by remember { mutableStateOf(currentRight) }
    var activeTab by remember { mutableStateOf(0) } // 0: Esquerdo, 1: Direito

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF090F19),
        title = {
            Column {
                Text(
                    text = "PERSONALIZAR PÁGINA $pageNumber",
                    color = Color(0xFF00FFCC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Escolha os Medidores",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                // Abas Esquerdo / Direito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) Color(0xFF00E5FF) else Color(0xFF162235)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Medidor 1: ${selectedLeft.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) Color.Black else Color.White
                        )
                    }
                    Button(
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) Color(0xFF00FF66) else Color(0xFF162235)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Medidor 2: ${selectedRight.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) Color.Black else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de opções selecionáveis
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GaugeType.values().forEach { gauge ->
                        val isCurrentSelected = if (activeTab == 0) selectedLeft == gauge else selectedRight == gauge
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (activeTab == 0) selectedLeft = gauge else selectedRight = gauge
                                },
                            color = if (isCurrentSelected) Color(0xFF162A40) else Color(0xFF0D1624),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentSelected) Color(0xFF00FFCC) else Color(0xFF1E293B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(gauge.icon, contentDescription = null, tint = gauge.color, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "${gauge.title} (${gauge.unit})",
                                        color = if (isCurrentSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isCurrentSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedLeft, selectedRight) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Salvar Ajustes", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}
