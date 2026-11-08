package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.TelemetryState
import com.example.obd.ObdManager
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    telemetry: TelemetryState,
    connectionLogs: List<String>,
    btDevices: List<ObdManager.BtDeviceInfo>,
    selectedBtAddress: String?,
    btEnabled: Boolean,
    isPowerInverted: Boolean = false,
    onTogglePowerInvert: () -> Unit = {},
    isDemo: Boolean = false,
    onToggleDemo: () -> Unit = {},
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String?) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "CONFIGURAÇÕES DO SISTEMA",
                color = ElectricBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Conectividade OBD-2 Real",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Status de conexão
        CockpitCard(title = "STATUS DA CONEXÃO", icon = Icons.Default.Bluetooth) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val statusText = when (telemetry.connectionState) {
                    ConnectionState.DISCONNECTED -> "Desconectado"
                    ConnectionState.SEARCHING -> "Procurando adaptador..."
                    ConnectionState.CONNECTING -> "Conectando..."
                    ConnectionState.CONNECTED -> "Conectado — dados reais"
                    ConnectionState.ERROR -> "Erro de conexão"
                }
                val statusColor = when (telemetry.connectionState) {
                    ConnectionState.CONNECTED -> StatusGreen
                    ConnectionState.ERROR -> StatusRed
                    ConnectionState.SEARCHING, ConnectionState.CONNECTING -> StatusAmber
                    else -> TextSecondary
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = if (btEnabled) "Bluetooth do sistema: LIGADO" else "Bluetooth do sistema: DESLIGADO",
                    color = if (btEnabled) StatusGreen else StatusRed,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (telemetry.isConnected) {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desconectar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onConnect,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.weight(1f),
                            enabled = telemetry.connectionState != ConnectionState.CONNECTING &&
                                    telemetry.connectionState != ConnectionState.SEARCHING
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (telemetry.connectionState == ConnectionState.CONNECTING ||
                                    telemetry.connectionState == ConnectionState.SEARCHING
                                ) "Conectando..." else "Conectar OBD-2",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Dispositivos Bluetooth
        CockpitCard(title = "DISPOSITIVOS BLUETOOTH PAREADOS", icon = Icons.Default.Devices) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ElectricBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atualizar lista", color = ElectricBlue, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, ElectricBlue)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir Bluetooth", color = ElectricBlue, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "Selecione o adaptador OBD (ELM327 / ADB) na lista abaixo e depois toque em Conectar.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (btDevices.isEmpty()) {
                    Text(
                        text = "Nenhum dispositivo pareado.\n1. Toque em \"Abrir Bluetooth\"\n2. Pareie o adaptador ELM327 / ADB\n3. Volte e toque em \"Atualizar lista\"",
                        color = StatusAmber,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(btDevices) { device ->
                            val selected = device.address == selectedBtAddress
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDevice(device.address) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) ElectricBlue.copy(alpha = 0.18f) else CockpitSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricBlue else CockpitCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (device.isLikelyObd) Icons.Default.DirectionsCar else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (selected) ElectricBlue else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = device.name,
                                                color = TextPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = device.address + if (device.isLikelyObd) "  •  provável OBD" else "",
                                                color = if (device.isLikelyObd) StatusGreen else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selecionado",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sistema Híbrido & Fluxo de Energia
        CockpitCard(title = "SISTEMA HÍBRIDO & FLUXO DE ENERGIA", icon = Icons.Default.Bolt) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inverter sinal da potência (kW)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Por convenção: kW (+) = descarga/tração e kW (-) = regeneração/recarga. Ative caso sua ECU registre o inverso.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isPowerInverted,
                        onCheckedChange = { onTogglePowerInvert() }
                    )
                }

                Divider(color = CockpitCardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Demonstração (Simulador)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Simula o ciclo dinâmico completo: STANDBY → EV → HÍBRIDO → COMBUSTÃO → CARREGANDO para testes da tela de fluxo.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isDemo,
                        onCheckedChange = { onToggleDemo() }
                    )
                }
            }
        }

        // Como conectar
        CockpitCard(title = "COMO CONECTAR", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("1. Toque em \"Abrir Bluetooth\" e pareie o adaptador ELM327 / ADB.", color = TextSecondary, fontSize = 13.sp)
                Text("2. Volte ao app e toque em \"Atualizar lista\".", color = TextSecondary, fontSize = 13.sp)
                Text("3. Selecione o dispositivo na lista (marcado como \"provável OBD\" se o nome bater).", color = TextSecondary, fontSize = 13.sp)
                Text("4. Toque em \"Conectar OBD-2\".", color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Logs
        CockpitCard(title = "LOG DE CONEXÃO", icon = Icons.Default.List) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(connectionLogs) { log ->
                    Text(
                        text = "• $log",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        CockpitCard(title = "SOBRE O APLICATIVO", icon = Icons.Default.Info) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "CIBA AUTO v1.2.0", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "Cockpit Digital Automotivo & Telemetria OBD-2 Real", color = TextSecondary, fontSize = 12.sp)
                Text(text = "Sem simulação. Dados apenas do adaptador físico.", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}


