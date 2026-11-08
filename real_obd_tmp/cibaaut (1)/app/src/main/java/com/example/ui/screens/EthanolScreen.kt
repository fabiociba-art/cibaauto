package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.ui.theme.*

@Composable
fun EthanolScreen(
    telemetry: TelemetryState,
    ethanolHistory: List<Pair<String, Int>>
) {
  val scrollState = rememberScrollState()
  val ethanolPct = telemetry.ethanolPercentage
  val gasolinePct = 100 - ethanolPct

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .verticalScroll(scrollState)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Column {
      Text(
          text = "MONITOR DE COMBUSTÍVEL FLEX",
          color = ElectricBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
      )
      Text(
          text = "Análise de Etanol e Gasolina",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
      )
    }

    // Main Ethanol Gauge Card
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
      Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = ElectricBlue)
            Text(text = "PROPORÇÃO DA MISTURA", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Surface(
              shape = RoundedCornerShape(8.dp),
              color = ElectricBlue.copy(alpha = 0.15f)
          ) {
            Text(
                text = "PID Mode 01 0x52",
                color = ElectricBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "ETANOL", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "$ethanolPct%", color = ElectricBlue, fontSize = 38.sp, fontWeight = FontWeight.Black)
          }
          Box(
              modifier = Modifier
                  .height(60.dp)
                  .width(1.dp)
                  .background(CockpitCardBorder)
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "GASOLINA", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "$gasolinePct%", color = NeonCyan, fontSize = 38.sp, fontWeight = FontWeight.Black)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar split
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)),
          ) {
            Box(
                modifier = Modifier
                    .weight(ethanolPct.toFloat())
                    .fillMaxHeight()
                    .background(ElectricBlue)
            )
            Box(
                modifier = Modifier
                    .weight(gasolinePct.toFloat())
                    .fillMaxHeight()
                    .background(NeonCyan)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Etanol ($ethanolPct%)", color = ElectricBlue, fontSize = 11.sp)
            Text(text = "Gasolina ($gasolinePct%)", color = NeonCyan, fontSize = 11.sp)
          }
        }
      }
    }

    // Technical details card
    CockpitCard(title = "DIAGNÓSTICO DO COMBUSTÍVEL", icon = Icons.Default.Info) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FuelDetailRow(label = "Tipo Identificado", value = "E85 / Flex Toyota Dual VVT-i")
        HorizontalDivider(color = CockpitCardBorder)
        FuelDetailRow(label = "Origem da Leitura", value = "Sensor Óptico / PID 0x52 (ECU)")
        HorizontalDivider(color = CockpitCardBorder)
        FuelDetailRow(label = "Qualidade da Mistura", value = "Excelente (Dentro da tolerância)")
        HorizontalDivider(color = CockpitCardBorder)
        FuelDetailRow(label = "Última Calibração", value = "Há 12 km (Automática via ECU)")
      }
    }

    // Historical variation chart card
    CockpitCard(title = "HISTÓRICO DA CONCENTRAÇÃO", icon = Icons.Default.Timeline) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Variação recente da porcentagem de etanol nas últimas leituras:",
            color = TextSecondary,
            fontSize = 12.sp
        )

        ethanolHistory.forEach { (time, value) ->
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = time, color = TextPrimary, fontSize = 13.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
              LinearProgressIndicator(
                  progress = { value / 100f },
                  modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                  color = ElectricBlue,
                  trackColor = CockpitSurfaceVariant
              )
            }
            Text(text = "$value%", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
fun FuelDetailRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = TextSecondary, fontSize = 13.sp)
    Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  }
}
