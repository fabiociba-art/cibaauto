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
fun TripScreen() {
  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text(
          text = "COMPUTADOR DE BORDO",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "Estatísticas da Viagem Atual",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    // Main Trip Distance Card
    CockpitCard(title = "DISTÂNCIA PERCORRIDA", icon = Icons.Default.Navigation) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "42.8", color = ElectricBlue, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text(text = "KM", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
          TripStatItem("Duração", "48 min")
          TripStatItem("Vel. Média", "53.5 km/h")
          TripStatItem("Vel. Máxima", "118 km/h")
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "COMBUSTÍVEL USADO",
          value = "3.8 L",
          subtitle = "Etanol E72",
          statusColor = StatusGreen,
          icon = Icons.Default.LocalGasStation
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CUSTO ESTIMADO",
          value = "R$ 18,24",
          subtitle = "R$ 4,79 / Litro",
          statusColor = NeonCyan,
          icon = Icons.Default.AttachMoney
      )
    }
  }
}

@Composable
fun TripStatItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextSecondary, fontSize = 11.sp)
    Text(text = value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
  }
}
