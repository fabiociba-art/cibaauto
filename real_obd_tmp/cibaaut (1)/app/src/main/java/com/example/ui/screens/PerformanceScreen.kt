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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PerformanceScreen() {
  var isMeasuring by remember { mutableStateOf(false) }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text(
          text = "CRONÔMETRO DE DESEMPENHO",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "Aceleração e Prova de Pista",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    // Main Chrono Card
    CockpitCard(title = "ACELERAÇÃO 0 - 100 KM/H", icon = Icons.Default.Timer) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
            text = if (isMeasuring) "MEDINDO..." else "9.2s",
            color = if (isMeasuring) StatusAmber else ElectricBlue,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black
        )
        Text(text = "Melhor marca registrada", color = TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { isMeasuring = !isMeasuring },
            colors = ButtonDefaults.buttonColors(containerColor = if (isMeasuring) StatusRed else ElectricBlue),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
              imageVector = if (isMeasuring) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              tint = CockpitBlack
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = if (isMeasuring) "Parar Medição" else "Iniciar Nova Medição (0-100)",
              color = CockpitBlack,
              fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Other benchmarks
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "1/4 DE MILHA",
          value = "16.5s",
          subtitle = "Velocidade: 132 km/h",
          statusColor = NeonCyan,
          icon = Icons.Default.Flag
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "VELOCIDADE MÁX",
          value = "185 km/h",
          subtitle = "Registrada hoje",
          statusColor = StatusAmber,
          icon = Icons.Default.Speed
      )
    }
  }
}
