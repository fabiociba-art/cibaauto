package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.ui.theme.*

@Composable
fun ChartsScreen(telemetry: TelemetryState) {
  var isPaused by remember { mutableStateOf(false) }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
            text = "TELEMETRIA GRÁFICA",
            color = ElectricBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Gráficos Comparativos em Tempo Real",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
      }

      IconButton(
          onClick = { isPaused = !isPaused },
          colors = IconButtonDefaults.iconButtonColors(containerColor = CockpitSurfaceVariant)
      ) {
        Icon(
            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            tint = ElectricBlue
        )
      }
    }

    // RPM Chart Simulation Card
    CockpitCard(title = "ROTAÇÃO DO MOTOR (RPM)", icon = Icons.Default.ShowChart) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Atual: ${telemetry.rpm} RPM", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text(text = if (isPaused) "PAUSADO" else "AO VIVO", color = if (isPaused) StatusAmber else StatusGreen, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(CockpitSurfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
          // Simulated waveform representation
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
          ) {
            val bars = listOf(30, 45, 60, 40, 75, 90, 65, 80, 55, 70, 85, 95, 60, 70, 85)
            bars.forEach { h ->
              Box(
                  modifier = Modifier
                      .width(10.dp)
                      .height((h * 1.2f).dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(if (h > 80) StatusAmber else ElectricBlue)
              )
            }
          }
        }
      }
    }

    // Speed Chart Simulation Card
    CockpitCard(title = "VELOCIDADE (KM/H)", icon = Icons.Default.Timeline) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "Atual: ${telemetry.speed.toInt()} km/h", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Text(text = "Média: 64 km/h", color = TextSecondary, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(CockpitSurfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
          ) {
            val bars = listOf(20, 30, 50, 70, 85, 80, 75, 65, 70, 80, 90, 85, 70, 60, 50)
            bars.forEach { h ->
              Box(
                  modifier = Modifier
                      .width(10.dp)
                      .height((h * 1.2f).dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(NeonCyan)
              )
            }
          }
        }
      }
    }
  }
}
