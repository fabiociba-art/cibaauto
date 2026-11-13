package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DTCLevel
import com.example.model.DiagnosticsState
import com.example.model.DtcEntry
import com.example.model.DtcKind
import com.example.model.ObdConnectionState
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import com.example.model.toReport
import com.example.ui.components.EngineSensorsCard
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitCardBorder
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun DTCLevel.color(): Color = when (this) {
    DTCLevel.CRITICAL -> GaugeRed
    DTCLevel.WARNING -> GaugeYellow
    DTCLevel.INFO -> TextMuted
}

private fun DTCLevel.label(): String = when (this) {
    DTCLevel.CRITICAL -> "GRAVE"
    DTCLevel.WARNING -> "ATENÇÃO"
    DTCLevel.INFO -> "INFO"
}

private fun formatTime(ms: Long): String =
    if (ms <= 0L) "nunca" else SimpleDateFormat("dd/MM HH:mm:ss", Locale("pt", "BR")).format(Date(ms))

@Composable
fun DiagnosticsScreen(
    diagnostics: DiagnosticsState,
    telemetry: TelemetryData,
    profile: VehicleProfile,
    connectionState: ObdConnectionState,
    isDemoMode: Boolean,
    onScan: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val canRead = isDemoMode || connectionState == ObdConnectionState.CONNECTED
    var selectedKind by remember { mutableStateOf(DtcKind.STORED) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Apagar códigos de falha?") },
            text = {
                Text(
                    "Isso apaga os códigos armazenados e apaga a luz de injeção, mas também zera os " +
                        "testes de prontidão (inspeção veicular pode exigir nova rodagem). " +
                        "Códigos permanentes só saem quando a ECU confirma o reparo. " +
                        "Se o defeito continuar, a luz volta a acender. Use com a ignição ligada e o carro parado."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClear()
                }) { Text("Apagar", color = GaugeRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(horizontal = 14.dp)
            .testTag("diagnostics_screen")
    ) {
        // Cabeçalho
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextWhite)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Diagnóstico do Veículo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(profile.name, fontSize = 11.sp, color = TextMuted)
            }
            IconButton(
                onClick = {
                    val report = diagnostics.toReport(profile.name, formatTime(diagnostics.lastScanMs))
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartilhar relatório"))
                },
                enabled = diagnostics.lastScanMs > 0L
            ) {
                Icon(
                    Icons.Default.Share, contentDescription = "Compartilhar relatório",
                    tint = if (diagnostics.lastScanMs > 0L) TextWhite else TextMuted
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Estado geral + botões
            item {
                StatusCard(
                    diagnostics = diagnostics,
                    canRead = canRead,
                    isDemoMode = isDemoMode,
                    onScan = onScan,
                    onClear = { showClearDialog = true }
                )
            }

            diagnostics.message?.let { msg ->
                item {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = GaugeYellow,
                        modifier = Modifier.testTag("diag_message")
                    )
                }
            }

            // Verificações rápidas (dados ao vivo)
            item { QuickChecks(telemetry, profile) }

            // Sensores adicionais do motor (somente os que a ECU respondeu)
            item { EngineSensorsCard(telemetry) }

            // Abas: armazenados / pendentes / permanentes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DtcKind.values().forEach { kind ->
                        val active = kind == selectedKind
                        val count = diagnostics.list(kind).size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFF1E2430) else CockpitCard)
                                .clickable { selectedKind = kind }
                                .padding(vertical = 10.dp)
                                .testTag("diag_tab_${kind.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${kind.label} ($count)",
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) TextWhite else TextMuted
                            )
                        }
                    }
                }
            }

            val shown = diagnostics.list(selectedKind)
            if (shown.isEmpty()) {
                item {
                    Text(
                        text = when {
                            diagnostics.lastScanMs == 0L -> "Toque em \"Ler códigos\" para consultar a ECU."
                            else -> "Nenhum código ${selectedKind.label.lowercase()}."
                        },
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(shown, key = { "${it.kind.name}_${it.code}" }) { entry ->
                    DtcCard(entry)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(
    diagnostics: DiagnosticsState,
    canRead: Boolean,
    isDemoMode: Boolean,
    onScan: () -> Unit,
    onClear: () -> Unit
) {
    val milColor = when (diagnostics.milOn) {
        true -> GaugeRed
        false -> GaugeGreen
        null -> TextMuted
    }
    val milText = when (diagnostics.milOn) {
        true -> "Luz de injeção ACESA"
        false -> "Luz de injeção apagada"
        null -> "Luz de injeção: não lida"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitCard),
        border = BorderStroke(1.dp, CockpitCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(milColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(milText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ECU informa ${diagnostics.reportedCount} código(s) confirmado(s)  •  Última leitura: ${formatTime(diagnostics.lastScanMs)}",
                fontSize = 11.sp,
                color = TextMuted
            )
            if (isDemoMode) {
                Text("MODO DEMONSTRAÇÃO: códigos simulados.", fontSize = 11.sp, color = GaugeYellow)
            } else if (!canRead) {
                Text("Conecte o adaptador OBD em Configurações para ler os códigos.", fontSize = 11.sp, color = GaugeYellow)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScan,
                    enabled = canRead && !diagnostics.isScanning,
                    modifier = Modifier.weight(1f).testTag("diag_scan_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GaugeGreen, contentColor = CockpitBlack)
                ) {
                    if (diagnostics.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CockpitBlack
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lendo...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ler códigos", fontSize = 13.sp)
                    }
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = canRead && !diagnostics.isScanning && diagnostics.hasFaults,
                    modifier = Modifier.weight(1f).testTag("diag_clear_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apagar", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DtcCard(entry: DtcEntry) {
    var expanded by remember { mutableStateOf(entry.info.severity == DTCLevel.CRITICAL) }
    val color = entry.info.severity.color()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("dtc_${entry.code}"),
        colors = CardDefaults.cardColors(containerColor = CockpitCard),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.code, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.info.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(entry.info.system, fontSize = 10.sp, color = TextMuted)
                }
                Text(
                    text = entry.info.severity.label(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(entry.info.description, fontSize = 13.sp, color = TextWhite)
            if (!entry.isKnown) {
                Text(
                    "Código fora da biblioteca: descrição genérica pelo tipo. Consulte o manual do veículo.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            if (expanded) {
                if (entry.info.possibleCauses.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Causas prováveis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(entry.info.possibleCauses, fontSize = 12.sp, color = TextWhite)
                }
                if (entry.info.recommendation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("O que fazer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Text(entry.info.recommendation, fontSize = 12.sp, color = TextWhite)
                }
            } else if (entry.info.possibleCauses.isNotBlank() || entry.info.recommendation.isNotBlank()) {
                Text("Toque para ver causas e recomendação", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

private data class QuickCheck(val label: String, val value: String, val status: String, val color: Color)

@Composable
private fun QuickChecks(telemetry: TelemetryData, profile: VehicleProfile) {
    val checks = buildList {
        val v = telemetry.batteryVoltage
        add(
            when {
                v <= 0.1f -> QuickCheck("Bateria 12 V", "--", "sem leitura", TextMuted)
                v < 11.8f -> QuickCheck("Bateria 12 V", "%.1f V".format(v), "muito baixa", GaugeRed)
                v < 12.2f -> QuickCheck("Bateria 12 V", "%.1f V".format(v), "baixa", GaugeYellow)
                v > 15.0f -> QuickCheck("Bateria 12 V", "%.1f V".format(v), "sobretensão", GaugeRed)
                else -> QuickCheck("Bateria 12 V", "%.1f V".format(v), "normal", GaugeGreen)
            }
        )
        val c = telemetry.coolantTempC
        add(
            when {
                c.isNaN() -> QuickCheck("Água do motor", "--", "sem leitura", TextMuted)
                c >= 108f -> QuickCheck("Água do motor", "${c.toInt()} °C", "superaquecimento", GaugeRed)
                c >= 100f -> QuickCheck("Água do motor", "${c.toInt()} °C", "alta", GaugeYellow)
                else -> QuickCheck("Água do motor", "${c.toInt()} °C", "normal", GaugeGreen)
            }
        )
        val f = telemetry.fuelLevelPercent
        add(
            when {
                !telemetry.fuelLevelAvailable -> QuickCheck("Combustível", "--", "sem leitura", TextMuted)
                f < 5f -> QuickCheck("Combustível", "${f.toInt()}%", "reserva crítica", GaugeRed)
                f < 12f -> QuickCheck("Combustível", "${f.toInt()}%", "na reserva", GaugeYellow)
                else -> QuickCheck("Combustível", "${f.toInt()}%", "normal", GaugeGreen)
            }
        )
        if (profile.isHybrid) {
            val soc = telemetry.hybridBatterySocPercent
            add(
                when {
                    !telemetry.hybridSocAvailable -> QuickCheck("Bateria híbrida (SOC)", "--", "não disponível via OBD", TextMuted)
                    soc < 20f -> QuickCheck("Bateria híbrida (SOC)", "${soc.toInt()}%", "muito baixa", GaugeRed)
                    soc < 35f -> QuickCheck("Bateria híbrida (SOC)", "${soc.toInt()}%", "baixa", GaugeYellow)
                    else -> QuickCheck("Bateria híbrida (SOC)", "${soc.toInt()}%", "normal", GaugeGreen)
                }
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitCard),
        border = BorderStroke(1.dp, CockpitCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Verificações rápidas (ao vivo)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(6.dp))
            checks.forEach { chk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (chk.color == GaugeGreen) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = chk.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(chk.label, fontSize = 12.sp, color = TextWhite, modifier = Modifier.weight(1f))
                    Text("${chk.value}  •  ${chk.status}", fontSize = 12.sp, color = chk.color)
                }
            }
        }
    }
}
