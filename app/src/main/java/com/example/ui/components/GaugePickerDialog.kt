package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GaugeCatalog
import com.example.model.GaugeSensor
import com.example.model.SensorCategory
import com.example.model.TelemetryData
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

private sealed class PickerRow {
    data class Header(val title: String) : PickerRow()
    data class Item(val sensor: GaugeSensor) : PickerRow()
}

/** Lista de sensores para trocar o manômetro tocado. Sensores com leitura atual aparecem primeiro. */
@Composable
fun GaugePickerDialog(
    currentId: String,
    telemetry: TelemetryData,
    onSelect: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val groupOrder = listOf(GaugeCatalog.GROUP_MAIN) + SensorCategory.values().map { it.label }
    val rows = buildList<PickerRow> {
        groupOrder.forEach { group ->
            val inGroup = GaugeCatalog.all
                .filter { it.group == group }
                .sortedByDescending { it.valueOf(telemetry) != null }
            if (inGroup.isNotEmpty()) {
                add(PickerRow.Header(group))
                inGroup.forEach { add(PickerRow.Item(it)) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CockpitCard,
        title = { Text("Escolher sensor", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(rows) { row ->
                    when (row) {
                        is PickerRow.Header -> Text(
                            text = row.title,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                        is PickerRow.Item -> {
                            val sensor = row.sensor
                            val value = sensor.valueOf(telemetry)
                            val selected = sensor.id == currentId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
                                    .clickable { onSelect(sensor.id) }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sensor.name, color = TextWhite, fontSize = 13.sp)
                                    Text(
                                        text = sensor.originFor(telemetry).label,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = if (value == null) "sem leitura" else sensor.format(value),
                                    color = if (value == null) TextMuted else GaugeGreen,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onReset) { Text("Restaurar originais", color = TextWhite) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", color = TextMuted) }
        }
    )
}
