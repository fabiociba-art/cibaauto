package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.ui.theme.*
import java.util.Locale

/** Computador de bordo com dados REAIS da viagem atual (distância, consumo, tempo, custo). */
@Composable
fun TripScreen(telemetry: TelemetryState, onResetTrip: () -> Unit = {}) {
  var priceText by rememberSaveable { mutableStateOf("6,00") }
  val price = priceText.replace(',', '.').toFloatOrNull() ?: 0f
  val liters = telemetry.totalFuelConsumedLiters
  val cost = liters * price
  val minutes = telemetry.tripDurationSec / 60
  val seconds = telemetry.tripDurationSec % 60
  val costPerKm = if (telemetry.tripDistanceKm > 0.05f) cost / telemetry.tripDistanceKm else 0f

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
        Text("COMPUTADOR DE BORDO", color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Viagem Atual", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }
      OutlinedButton(onClick = onResetTrip) { Text("Zerar", fontSize = 12.sp) }
    }

    if (!telemetry.isConnected) {
      Text("Conecte ao OBD para acompanhar a viagem. Valores abaixo ficam zerados.", color = StatusAmber, fontSize = 12.sp)
    }

    CockpitCard(title = "DISTÂNCIA PERCORRIDA", icon = Icons.Default.Navigation) {
      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(String.format(Locale.US, "%.1f", telemetry.tripDistanceKm), color = ElectricBlue, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text("KM", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
          TripStatItem("Duração", String.format(Locale.US, "%d:%02d min", minutes, seconds))
          TripStatItem("Vel. Média", String.format(Locale.US, "%.1f km/h", telemetry.tripAvgSpeed))
          TripStatItem("Vel. Máxima", "${telemetry.tripMaxSpeed.toInt()} km/h")
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CONSUMO MÉDIO",
          value = if (telemetry.averageConsumption > 0f) String.format(Locale.US, "%.1f km/L", telemetry.averageConsumption) else "—",
          subtitle = "Na viagem",
          statusColor = StatusGreen,
          icon = Icons.Default.Speed
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CONSUMO AGORA",
          value = if (telemetry.instantConsumption > 0f) String.format(Locale.US, "%.1f km/L", telemetry.instantConsumption) else "—",
          subtitle = "Instantâneo",
          statusColor = NeonCyan,
          icon = Icons.Default.Bolt
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "COMBUSTÍVEL USADO",
          value = String.format(Locale.US, "%.2f L", liters),
          subtitle = telemetry.fuelTypeLabel,
          statusColor = StatusGreen,
          icon = Icons.Default.LocalGasStation
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CUSTO ESTIMADO",
          value = String.format(Locale.US, "R$ %.2f", cost),
          subtitle = if (costPerKm > 0f) String.format(Locale.US, "R$ %.2f / km", costPerKm) else "R$ / km: —",
          statusColor = NeonCyan,
          icon = Icons.Default.AttachMoney
      )
    }

    OutlinedTextField(
        value = priceText,
        onValueChange = { priceText = it },
        label = { Text("Preço do litro (R$)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )

    Text(
        "O consumo é estimado a partir do sensor de fluxo de ar (MAF) ou da pressão do coletor. No híbrido, trechos 100% elétricos não gastam combustível.",
        color = TextMuted,
        fontSize = 11.sp
    )
  }
}

@Composable
fun TripStatItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextSecondary, fontSize = 11.sp)
    Text(text = value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
  }
}
