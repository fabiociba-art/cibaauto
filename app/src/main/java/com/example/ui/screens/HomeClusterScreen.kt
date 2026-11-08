package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.TelemetryState

/**
 * Nova tela inicial de Cluster do CIBA AUTO.
 * Exibe velocímetro digital, conta-giros, odômetro, autonomia, bateria,
 * monitoramento de etanol e alertas do sistema.
 */
@Composable
fun HomeClusterScreen(telemetry: TelemetryState) {
    AndroidAutoClusterScreen(telemetry = telemetry)
}
