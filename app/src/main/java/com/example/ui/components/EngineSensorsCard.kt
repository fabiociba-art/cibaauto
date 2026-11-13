package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SensorCategory
import com.example.model.SensorRegistry
import com.example.model.TelemetryData
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

/**
 * Lista os sensores adicionais que a ECU realmente respondeu, agrupados por categoria.
 * "ƒ" = valor calculado pelo app a partir de leituras reais. Sensor sem resposta não aparece.
 */
@Composable
fun EngineSensorsCard(telemetry: TelemetryData, modifier: Modifier = Modifier) {
    val available = SensorRegistry.all.filter { telemetry.sensors.containsKey(it.id) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitCard),
        border = BorderStroke(1.dp, CockpitCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Sensores do motor (ao vivo)",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (available.isEmpty()) {
                Text(
                    "Nenhum sensor adicional lido ainda. Conecte ao adaptador OBD com o carro ligado.",
                    fontSize = 11.sp, color = TextMuted
                )
            } else {
                Text(
                    "${available.size} sensores respondendo  •  ƒ = calculado pelo app",
                    fontSize = 11.sp, color = TextMuted
                )
                SensorCategory.values().forEach { category ->
                    val items = available.filter { it.category == category }
                    if (items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        items.forEach { def ->
                            val value = telemetry.sensors[def.id]
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(def.name, fontSize = 12.sp, color = TextWhite, modifier = Modifier.weight(1f))
                                Text(
                                    text = if (value == null) "--" else def.format(value) + def.origin.marker,
                                    fontSize = 12.sp, color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
