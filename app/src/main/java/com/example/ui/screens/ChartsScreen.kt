package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartHistory
import com.example.model.TelemetryState
import com.example.ui.theme.*

/** Gráficos ao vivo com o histórico REAL (últimas ~120 leituras). Pausar congela a imagem. */
@Composable
fun ChartsScreen(telemetry: TelemetryState, history: ChartHistory = ChartHistory()) {
  var isPaused by remember { mutableStateOf(false) }
  var frozen by remember { mutableStateOf(history) }
  if (!isPaused) frozen = history
  val h = frozen

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("TELEMETRIA GRÁFICA", color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Gráficos em Tempo Real", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }
      IconButton(
          onClick = { isPaused = !isPaused },
          colors = IconButtonDefaults.iconButtonColors(containerColor = CockpitSurfaceVariant)
      ) {
        Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = ElectricBlue)
      }
    }

    if (!telemetry.isConnected && h.rpm.isEmpty()) {
      Text("Conecte ao OBD para ver os gráficos.", color = StatusAmber, fontSize = 12.sp)
    }

    ChartCard("ROTAÇÃO DO MOTOR (RPM)", "${telemetry.rpm} RPM", h.rpm, ElectricBlue, 0f, 6000f, isPaused)
    ChartCard("VELOCIDADE (KM/H)", "${telemetry.speed.toInt()} km/h", h.speed, NeonCyan, 0f, 160f, isPaused)
    ChartCard("CARGA DO MOTOR (%)", "${telemetry.engineLoad}%", h.load, StatusAmber, 0f, 100f, isPaused)
    ChartCard("TEMP. ARREFECIMENTO (°C)", "${telemetry.coolantTemp} °C", h.coolant, StatusGreen, 40f, 120f, isPaused)
  }
}

@Composable
private fun ChartCard(title: String, current: String, data: List<Float>, color: Color, minY: Float, maxY: Float, paused: Boolean) {
  CockpitCard(title = title, icon = Icons.Default.ShowChart) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Atual: $current", color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(if (paused) "PAUSADO" else "AO VIVO", color = if (paused) StatusAmber else StatusGreen, fontSize = 12.sp)
      }
      val top = maxOf(maxY, data.maxOrNull() ?: maxY)
      Canvas(
          modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .background(CockpitSurfaceVariant, RoundedCornerShape(10.dp))
      ) {
        if (data.size >= 2) {
          val stepX = size.width / (data.size - 1)
          val range = (top - minY).coerceAtLeast(1f)
          val path = Path()
          data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minY) / range).coerceIn(0f, 1f) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(path, color, style = Stroke(width = 3f))
        } else {
          drawLine(color.copy(alpha = 0.3f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2f)
        }
      }
      Text("Escala: ${minY.toInt()} a ${top.toInt()}", color = TextMuted, fontSize = 10.sp)
    }
  }
}
