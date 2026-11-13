package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.model.GaugeSensor
import com.example.model.TelemetryData

private fun centerTypeFor(sensorId: String): MiniGaugeCenterType = when (sensorId) {
    "oil_temp" -> MiniGaugeCenterType.ICON_OIL
    "coolant_temp" -> MiniGaugeCenterType.ICON_COOLANT
    "ethanol" -> MiniGaugeCenterType.ICON_ETHANOL
    "battery12" -> MiniGaugeCenterType.NEEDLE_BATTERY
    "hybrid_soc" -> MiniGaugeCenterType.NEEDLE_SOC
    else -> MiniGaugeCenterType.NEEDLE_TURBO
}

/**
 * Manômetro cujo sensor é escolhido pelo usuário. Toque = abrir a lista de sensores.
 * Sem leitura atual: mostra "--" e o ponteiro fica no mínimo. "ƒ" = calculado, "~" = estimado.
 */
@Composable
fun ConfigurableGauge(
    sensor: GaugeSensor,
    telemetry: TelemetryData,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val value = sensor.valueOf(telemetry)?.takeUnless { it.isNaN() }
    val origin = sensor.originFor(telemetry)

    MiniGauge(
        value = value ?: sensor.min,
        minValue = sensor.min,
        maxValue = sensor.max,
        label = sensor.shortName + origin.marker,
        unit = sensor.unit,
        centerType = centerTypeFor(sensor.id),
        displayValue = if (value == null) "--" else sensor.format(value),
        size = size,
        modifier = modifier.clickable(onClick = onClick)
    )
}
