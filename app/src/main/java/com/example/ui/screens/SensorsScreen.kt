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
import com.example.model.TelemetryState
import com.example.ui.theme.*

data class SensorInfo(val pid: String, val name: String, val value: String, val unit: String, val status: String)

@Composable
fun SensorsScreen(telemetry: TelemetryState) {
  val sensors = listOf(
      SensorInfo("010C", "Rotação do Motor (RPM)", "${telemetry.rpm}", "RPM", "Suportado"),
      SensorInfo("010D", "Velocidade do Veículo", "${telemetry.speed.toInt()}", "km/h", "Suportado"),
      SensorInfo("0105", "Temp. Líquido de Arrefecimento", "${telemetry.coolantTemp}", "°C", "Suportado"),
      SensorInfo("010F", "Temp. Ar de Admissão (IAT)", "${telemetry.intakeTemp}", "°C", "Suportado"),
      SensorInfo("0104", "Carga Calculada do Motor", "${telemetry.engineLoad}", "%", "Suportado"),
      SensorInfo("0142", "Tensão do Módulo de Controle", "${telemetry.batteryVoltage}", "V", "Suportado"),
      SensorInfo("010B", "Pressão Absoluta do Coletor (MAP)", "${telemetry.manifoldPressure}", "kPa", "Suportado"),
      SensorInfo("0111", "Posição do Acelerador (TPS)", "${telemetry.throttlePosition}", "%", "Suportado"),
      SensorInfo("0152", "Porcentagem de Etanol no Tanque", "${telemetry.ethanolPercentage}", "%", "Suportado"),
      SensorInfo("010A", "Pressão do Combustível", "${telemetry.fuelPressure.toInt()}", "kPa", "Suportado"),
      SensorInfo("015C", "Temperatura do Óleo", telemetry.oilTemp?.toString() ?: "—", "°C", if (telemetry.oilTemp != null) "Suportado" else "Não informado pelo carro"),
      SensorInfo("0146", "Temperatura Ambiente", telemetry.ambientTemp?.toString() ?: "—", "°C", if (telemetry.ambientTemp != null) "Suportado" else "Não informado pelo carro"),
      SensorInfo("013C", "Temperatura do Catalisador", telemetry.catalystTemp?.toString() ?: "—", "°C", if (telemetry.catalystTemp != null) "Suportado" else "Não informado pelo carro")
  )

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text(
          text = "SENSORES OBD-2 EM TEMPO REAL",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "PIDs Ativos da ECU",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(sensors) { sensor ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CockpitSurface,
            border = BorderStroke(1.dp, CockpitCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
          Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ElectricBlue.copy(alpha = 0.2f)
                ) {
                  Text(
                      text = sensor.pid,
                      color = ElectricBlue,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Text(text = sensor.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = "Status: ${sensor.status}", color = StatusGreen, fontSize = 11.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(text = sensor.value, color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
              Text(text = sensor.unit, color = TextSecondary, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}
