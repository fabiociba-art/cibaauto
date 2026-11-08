package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.TripRecord
import com.example.ui.theme.*

@Composable
fun HistoryScreen(trips: List<TripRecord>) {
  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text(
          text = "HISTÓRICO DE VIAGENS",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "Viagens e Registros Anteriores",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(trips) { trip ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CockpitSurface,
            border = BorderStroke(1.dp, CockpitCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Route, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                Text(text = trip.date, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
              Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = NeonCyan.copy(alpha = 0.2f)
              ) {
                Text(
                    text = "${trip.distanceKm} km",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
              TripDetailCell("Duração", "${trip.durationMinutes} min")
              TripDetailCell("Média", "${trip.avgSpeed} km/h")
              TripDetailCell("Consumo", "${trip.avgConsumption} km/L")
              TripDetailCell("Combustível", "${trip.fuelConsumedLiters} L")
            }
          }
        }
      }
    }
  }
}

@Composable
fun TripDetailCell(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextSecondary, fontSize = 10.sp)
    Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  }
}
