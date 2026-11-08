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
import com.example.model.DiagnosticCode
import com.example.model.DTCLevel
import com.example.ui.theme.*

@Composable
fun DiagnosticsScreen(
    diagnosticCodes: List<DiagnosticCode>,
    readinessMonitors: Map<String, String>,
    onClearCodes: () -> Unit
) {
  var showClearDialog by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(0) } // 0: DTCs, 1: Readiness & Freeze Frame

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
            text = "DIAGNÓSTICO AUTOMOTIVO",
            color = ElectricBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "ECU & Prontidão OBD-2",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
      }

      Button(
          onClick = { showClearDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
          border = BorderStroke(1.dp, StatusRed),
          shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Limpar DTCs", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Tabs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      TabButton(
          modifier = Modifier.weight(1f),
          title = "Códigos de Falha (${diagnosticCodes.size})",
          isSelected = selectedTab == 0,
          onClick = { selectedTab = 0 }
      )
      TabButton(
          modifier = Modifier.weight(1f),
          title = "Readiness & Freeze Frame",
          isSelected = selectedTab == 1,
          onClick = { selectedTab = 1 }
      )
    }

    if (selectedTab == 0) {
      // MIL Status Banner
      Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (diagnosticCodes.isEmpty()) StatusGreen.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, if (diagnosticCodes.isEmpty()) StatusGreen else StatusAmber),
          modifier = Modifier.fillMaxWidth()
      ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
              imageVector = if (diagnosticCodes.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (diagnosticCodes.isEmpty()) StatusGreen else StatusAmber
          )
          Column {
            Text(
                text = if (diagnosticCodes.isEmpty()) "Luz de Anomalia (MIL): Apagada" else "Luz de Anomalia (MIL): Ativa",
                color = if (diagnosticCodes.isEmpty()) StatusGreen else StatusAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = if (diagnosticCodes.isEmpty()) "Nenhum código crítico armazenado na ECU." else "${diagnosticCodes.size} código(s) detectado(s).",
                color = TextSecondary,
                fontSize = 11.sp
            )
          }
        }
      }

      if (diagnosticCodes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Nenhum erro encontrado na ECU", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "O sistema do veículo está operando normalmente.", color = TextSecondary, fontSize = 12.sp)
          }
        }
      } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(diagnosticCodes) { dtc ->
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusAmber.copy(alpha = 0.2f)
                    ) {
                      Text(
                          text = dtc.code,
                          color = StatusAmber,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                    Text(text = dtc.system, color = TextSecondary, fontSize = 11.sp)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = dtc.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dtc.description, color = TextSecondary, fontSize = 12.sp)
              }
            }
          }
        }
      }
    } else {
      // Readiness & Freeze Frame Tab
      LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          CockpitCard(title = "MONITORES DE PRONTIDÃO (READINESS)", icon = Icons.Default.TaskAlt) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              readinessMonitors.forEach { (monitor, status) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = monitor, color = TextPrimary, fontSize = 13.sp)
                  Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (status == "Completo") StatusGreen.copy(alpha = 0.2f) else StatusAmber.copy(alpha = 0.2f)
                  ) {
                    Text(
                        text = status,
                        color = if (status == "Completo") StatusGreen else StatusAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }
                HorizontalDivider(color = CockpitCardBorder)
              }
            }
          }
        }

        item {
          CockpitCard(title = "FREEZE FRAME (DADOS CONGELADOS P0130)", icon = Icons.Default.AcUnit) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              FreezeFrameRow("RPM no momento da falha", "2,450 RPM")
              FreezeFrameRow("Velocidade do veículo", "72 km/h")
              FreezeFrameRow("Temperatura do motor", "89 °C")
              FreezeFrameRow("Pressão do coletor (MAP)", "48 kPa")
              FreezeFrameRow("Correção de combustível STFT", "+3.2%")
            }
          }
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
        onDismissRequest = { showClearDialog = false },
        containerColor = CockpitSurface,
        title = { Text("Limpar Códigos DTC?", color = TextPrimary) },
        text = {
          Text(
              "Esta ação enviará o comando OBD para apagar os códigos de falha da ECU e desligar a luz MIL. Certifique-se de que o problema mecânico foi resolvido.",
              color = TextSecondary
          )
        },
        confirmButton = {
          Button(
              onClick = {
                onClearCodes()
                showClearDialog = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
          ) {
            Text("Sim, Limpar", color = TextPrimary)
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearDialog = false }) {
            Text("Cancelar", color = TextSecondary)
          }
        }
    )
  }
}

@Composable
fun TabButton(modifier: Modifier = Modifier, title: String, isSelected: Boolean, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier = modifier,
      colors = ButtonDefaults.buttonColors(
          containerColor = if (isSelected) ElectricBlue else CockpitSurface
      ),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, if (isSelected) ElectricBlue else CockpitCardBorder)
  ) {
    Text(
        text = title,
        color = if (isSelected) CockpitBlack else TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
  }
}

@Composable
fun FreezeFrameRow(label: String, value: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, color = TextSecondary, fontSize = 12.sp)
    Text(text = value, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}
