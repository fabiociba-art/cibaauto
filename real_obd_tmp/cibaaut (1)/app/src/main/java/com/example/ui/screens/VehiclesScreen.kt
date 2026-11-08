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
import com.example.model.PreloadedVehicles
import com.example.model.VehicleProfile
import com.example.ui.theme.*

@Composable
fun VehiclesScreen(
    selectedVehicle: VehicleProfile,
    onSelectVehicle: (VehicleProfile) -> Unit
) {
  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text(
          text = "PERFIS DE VEÍCULOS TOYOTA",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "Selecione ou Alterne o Veículo",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(PreloadedVehicles.all) { vehicle ->
        val isSelected = vehicle.id == selectedVehicle.id

        Surface(
            onClick = { onSelectVehicle(vehicle) },
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) CockpitSurfaceVariant else CockpitSurface,
            border = BorderStroke(1.dp, if (isSelected) ElectricBlue else CockpitCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = if (vehicle.isHybrid) Icons.Default.Bolt else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = if (vehicle.isHybrid) StatusGreen else ElectricBlue
                )
                Column {
                  Text(text = vehicle.fullName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  Text(text = vehicle.engine, color = TextSecondary, fontSize = 12.sp)
                }
              }

              if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricBlue
                ) {
                  Text(
                      text = "ATIVO",
                      color = CockpitBlack,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CockpitCardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
              VehicleSpecItem("Transmissão", vehicle.transmission)
              VehicleSpecItem("Combustível", vehicle.fuelType)
              VehicleSpecItem("Tanque", vehicle.tankCapacity)
            }

            if (vehicle.notes.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(text = vehicle.notes, color = TextMuted, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
fun VehicleSpecItem(label: String, value: String) {
  Column {
    Text(text = label, color = TextSecondary, fontSize = 10.sp)
    Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}
