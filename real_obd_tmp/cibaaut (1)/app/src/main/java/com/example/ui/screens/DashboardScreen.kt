package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TelemetryState
import com.example.model.VehicleProfile
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    vehicle: VehicleProfile,
    telemetry: TelemetryState,
    onNavigateToEthanol: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onToggleConnection: () -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .verticalScroll(scrollState)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Branded Header Banner using the uploaded logomark
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
      Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Image(
              painter = painterResource(id = R.drawable.ciba_icon),
              contentDescription = "CIBA AUTO Logo",
              modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
          )
          Column {
            Text(
                text = "CIBA AUTO",
                color = ElectricBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Text(
                text = vehicle.fullName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
          }
        }

        Surface(
            onClick = onNavigateToVehicles,
            shape = RoundedCornerShape(10.dp),
            color = CockpitSurfaceVariant,
            border = BorderStroke(1.dp, CockpitCardBorder)
        ) {
          Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            Text(text = "Trocar", color = TextPrimary, fontSize = 11.sp)
          }
        }
      }
    }

    // Connection Banner (apenas dados reais)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (telemetry.isConnected) StatusGreen.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, if (telemetry.isConnected) StatusGreen else StatusAmber)
    ) {
      Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
              modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(if (telemetry.isConnected) StatusGreen else StatusAmber)
          )
          Column {
            Text(
                text = if (telemetry.isConnected) "OBD-II CONECTADO (ELM327)" else "DESCONECTADO",
                color = if (telemetry.isConnected) StatusGreen else StatusAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = if (telemetry.isConnected) "Telemetria em tempo real do adaptador" else "Toque em Conectar para ler o ELM327",
                color = TextSecondary,
                fontSize = 11.sp
            )
          }
        }

        Button(
            onClick = onToggleConnection,
            colors = ButtonDefaults.buttonColors(containerColor = if (telemetry.isConnected) StatusRed else ElectricBlue),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
          Text(
              text = if (telemetry.isConnected) "Desconectar" else "Conectar",
              color = CockpitBlack,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
          )
        }
      }
    }

    // Main Central Gauges: Speedometer & Tachometer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Tachometer Card
      CockpitCard(
          modifier = Modifier.weight(1f),
          title = "CONTA-GIROS",
          icon = Icons.Default.Speed
      ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          Text(
              text = "${telemetry.rpm}",
              color = ElectricBlue,
              fontSize = 36.sp,
              fontWeight = FontWeight.Black
          )
          Text(text = "RPM", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
              progress = { (telemetry.rpm / 7000f).coerceIn(0f, 1f) },
              modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
              color = ElectricBlue,
              trackColor = CockpitSurfaceVariant,
          )
        }
      }

      // Speedometer Card
      CockpitCard(
          modifier = Modifier.weight(1f),
          title = "VELOCIDADE",
          icon = Icons.Default.Timeline
      ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          Text(
              text = "${telemetry.speed.toInt()}",
              color = NeonCyan,
              fontSize = 36.sp,
              fontWeight = FontWeight.Black
          )
          Text(text = "KM/H", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(
              progress = { (telemetry.speed / 220f).coerceIn(0f, 1f) },
              modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
              color = NeonCyan,
              trackColor = CockpitSurfaceVariant,
          )
        }
      }
    }

    // Ethanol Flex Monitor Highlight Card (Requested specially by user)
    Surface(
        onClick = onNavigateToEthanol,
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
      Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
              modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(ElectricBlue.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = ElectricBlue)
          }

          Column {
            Text(text = "COMBUSTÍVEL FLEX — ETANOL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "${telemetry.ethanolPercentage}% Etanol / ${100 - telemetry.ethanolPercentage}% Gasolina", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = telemetry.fuelTypeLabel, color = ElectricBlue, fontSize = 12.sp)
          }
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ElectricBlue)
      }
    }

    // Hybrid specific section if hybrid
    if (vehicle.isHybrid) {
      CockpitCard(title = "SISTEMA HÍBRIDO TOYOTA", icon = Icons.Default.Bolt) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TelemetryMetricItem("Bateria HV", "${telemetry.hybridBatterySoc}%", "Li-Ion", StatusGreen)
          TelemetryMetricItem("Motor Elétrico", "${telemetry.hybridMotorPowerKw} kW", "Ativo", ElectricBlue)
          TelemetryMetricItem("Modo", "ECO / EV", "Regenerativo", NeonCyan)
        }
      }
    }

    // Grid of Secondary Gauges
    Text(
        text = "PARÂMETROS DE TELEMETRIA",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "ÁGUA MOTOR",
          value = "${telemetry.coolantTemp}°C",
          subtitle = "Normal",
          statusColor = StatusGreen,
          icon = Icons.Default.Thermostat
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CONSUMO MÉDIO",
          value = "${telemetry.averageConsumption}",
          subtitle = "KM/L",
          statusColor = NeonCyan,
          icon = Icons.Default.Eco
      )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "CARGA MOTOR",
          value = "${telemetry.engineLoad}%",
          subtitle = "TPS Ativo",
          statusColor = ElectricBlue,
          icon = Icons.Default.Memory
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "TENSÃO BATERIA",
          value = "${telemetry.batteryVoltage}V",
          subtitle = "Alternador OK",
          statusColor = StatusGreen,
          icon = Icons.Default.BatteryChargingFull
      )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "AR ADMISSÃO",
          value = "${telemetry.intakeTemp}°C",
          subtitle = "IAT Sensor",
          statusColor = TextSecondary,
          icon = Icons.Default.Air
      )
      CockpitMetricCard(
          modifier = Modifier.weight(1f),
          title = "PRESSÃO COLETOR",
          value = "${telemetry.manifoldPressure.toInt()} kPa",
          subtitle = "MAP Sensor",
          statusColor = ElectricBlue,
          icon = Icons.Default.Speed
      )
    }
  }
}

@Composable
fun CockpitCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp),
      color = CockpitSurface,
      border = BorderStroke(1.dp, CockpitCardBorder)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
        Text(text = title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
      }
      Spacer(modifier = Modifier.height(10.dp))
      content()
    }
  }
}

@Composable
fun CockpitMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    statusColor: Color,
    icon: ImageVector
) {
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(16.dp),
      color = CockpitSurface,
      border = BorderStroke(1.dp, CockpitCardBorder)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
      Text(text = subtitle, color = statusColor, fontSize = 11.sp)
    }
  }
}

@Composable
fun TelemetryMetricItem(label: String, value: String, sub: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextSecondary, fontSize = 10.sp)
    Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text(text = sub, color = TextMuted, fontSize = 10.sp)
  }
}
