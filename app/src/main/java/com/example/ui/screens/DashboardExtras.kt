package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryState
import com.example.obd.ObdManager
import com.example.ui.theme.*

/** Estimativa do fluxo de energia a partir de RPM, velocidade e carga (não há PID padrão para isso). */
fun hybridFlowLabel(t: TelemetryState): String {
  if (!t.isConnected) return "—"
  return when {
    t.rpm == 0 && t.speed <= 0f -> "Pronto (motor desligado)"
    t.rpm == 0 -> "100% elétrico (EV)"
    t.speed <= 1f -> "Motor ligado, parado (carregando bateria)"
    t.engineLoad >= 70 || t.throttlePosition >= 70 -> "Motor + elétrico (aceleração forte)"
    else -> "Motor a combustão (cruzeiro / carga)"
  }
}

@Composable
fun HybridFlowCard(telemetry: TelemetryState, onOpenFlow: () -> Unit = {}) {
  val isEv = telemetry.isConnected && telemetry.rpm == 0 && telemetry.speed > 0f
  CockpitCard(title = "MODO E FLUXO DE ENERGIA (ESTIMADO)", icon = Icons.Default.Bolt) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TelemetryMetricItem(
            "Modo",
            if (!telemetry.isConnected) "—" else if (isEv) "EV" else if (telemetry.rpm > 0) "Motor" else "Parado",
            if (isEv) "Rodando só no elétrico" else "Baseado em RPM = 0",
            if (isEv) StatusGreen else ElectricBlue
        )
        TelemetryMetricItem(
            "Motor a combustão",
            if (!telemetry.isConnected) "—" else if (telemetry.rpm > 0) "LIGADO" else "DESLIGADO",
            "${telemetry.rpm} RPM",
            if (telemetry.rpm > 0) StatusAmber else StatusGreen
        )
      }
      Text(text = hybridFlowLabel(telemetry), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

      Button(
          onClick = onOpenFlow,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.2f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFCC))
      ) {
        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Ver Cockpit Animado de Fluxo", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      if (telemetry.hybridExtras.isEmpty()) {
        Text(
            text = "Tensão, corrente e temperaturas da bateria HV/inversor aguardam o mapeamento dos PIDs do seu Corolla (use a varredura do híbrido).",
            color = TextMuted,
            fontSize = 11.sp
        )
      } else {
        telemetry.hybridExtras.forEach { (k, v) ->
          val parts = k.split("|")
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = parts.getOrElse(0) { k }, color = TextSecondary, fontSize = 12.sp)
            Text(text = "$v ${parts.getOrElse(1) { "" }}", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun ExtraTempsCard(telemetry: TelemetryState) {
  fun f(v: Int?) = if (!telemetry.isConnected) "—" else if (v == null) "n/d" else "$v °C"
  CockpitCard(title = "TEMPERATURAS", icon = Icons.Default.Thermostat) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TelemetryMetricItem("Arrefecimento", if (telemetry.isConnected) "${telemetry.coolantTemp} °C" else "—", "PID 05", ElectricBlue)
        TelemetryMetricItem("Admissão", if (telemetry.isConnected) "${telemetry.intakeTemp} °C" else "—", "PID 0F", ElectricBlue)
        TelemetryMetricItem("Ambiente", f(telemetry.ambientTemp), "PID 46", NeonCyan)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TelemetryMetricItem("Óleo", f(telemetry.oilTemp), "PID 5C", StatusAmber)
        TelemetryMetricItem("Catalisador", f(telemetry.catalystTemp), "PID 3C", StatusAmber)
      }
      Text(text = "n/d = o carro não informa esse sensor pelo OBD padrão.", color = TextMuted, fontSize = 10.sp)
    }
  }
}

@Composable
fun VehicleInfoCard(
    info: ObdManager.VehicleInfo?,
    loading: Boolean,
    connected: Boolean,
    onRead: () -> Unit
) {
  CockpitCard(title = "INFORMAÇÕES DO VEÍCULO", icon = Icons.Default.Info) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      InfoRow("VIN (chassi)", info?.vin?.ifBlank { "n/d" } ?: "—")
      InfoRow("ID de calibração", info?.calibrationId?.ifBlank { "n/d" } ?: "—")
      InfoRow("Combustível (ECU)", info?.fuelType?.ifBlank { "n/d" } ?: "—")
      OutlinedButton(
          onClick = onRead,
          enabled = connected && !loading,
          modifier = Modifier.fillMaxWidth()
      ) { Text(if (loading) "Lendo..." else "Ler informações do veículo", fontSize = 12.sp) }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(text = label, color = TextSecondary, fontSize = 12.sp)
    Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun ShortcutsRow(onTrip: () -> Unit, onCharts: () -> Unit, onSensors: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(onClick = onTrip, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Viagem", fontSize = 12.sp) }
    OutlinedButton(onClick = onCharts, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Gráficos", fontSize = 12.sp) }
    OutlinedButton(onClick = onSensors, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Sensores", fontSize = 12.sp) }
  }
}
