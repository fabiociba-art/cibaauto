package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.model.TelemetryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tela do Monitor de Combustível Flex (Etanol & Gasolina) para Android Auto.
 * Fornece a proporção da mistura, autonomia estimada por combustível e análise de rendimento.
 */
class CibaAutoEthanolCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()
    private var lastUpdateMs: Long = 0L

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    TelemetryBridge.telemetry.collectLatest { state ->
                        latest = state
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMs >= 1000L) {
                            lastUpdateMs = now
                            invalidate()
                        }
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                job?.cancel()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val t = latest
        val ethanolPct = t.ethanolPercentage
        val gasolinePct = (100 - ethanolPct).coerceIn(0, 100)

        val fuelDescription = when {
            ethanolPct <= 10 -> "Gasolina Pura (E$ethanolPct)"
            ethanolPct in 11..35 -> "Gasolina Comercial E27 ($ethanolPct% Etanol)"
            ethanolPct in 36..75 -> "Mistura Flex Balanceada ($ethanolPct% Etanol)"
            else -> "Etanol Hidratado Predominante ($ethanolPct%)"
        }

        // Estimativa de autonomia com tanque atual
        val baseRange = if (t.rangeKm > 0) t.rangeKm else 450
        val estEthanolRange = (baseRange * 0.72f).toInt()
        val estGasolineRange = (baseRange * 1.05f).toInt()

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("$ethanolPct% Etanol  |  $gasolinePct% Gasolina")
                    .addText("Composição da Mistura no Tanque (Sensor Flex)")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(fuelDescription)
                    .addText("Classificação da ECU")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Autonomia Estimada: ~$estGasolineRange km (Gas.) / ~$estEthanolRange km (Eta.)")
                    .addText("Projeção comparativa de alcance por tipo de combustível")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Regra dos 70%: Etanol compensa se custar até 70% da Gasolina")
                    .addText("Ponto de equilíbrio térmico para motor Flex")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Vazão: ${String.format("%.1f", t.fuelFlowLitersPerHour)} L/h  |  Médio: ${String.format("%.1f", t.averageConsumption)} km/L")
                    .addText("Consumo do motor em regime atual")
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Monitor de Combustível Flex")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
