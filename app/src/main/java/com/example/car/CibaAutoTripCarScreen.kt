package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
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
 * Computador de Bordo e dados da Viagem Atual para Android Auto.
 * Exibe odômetro, consumo médio, velocidade média/máxima, tempo e botão para zerar viagem.
 */
class CibaAutoTripCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()
    private var lastUpdateMs: Long = 0L
    private var showConfirmResetDialog: Boolean = false

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
        if (showConfirmResetDialog) {
            return MessageTemplate.Builder("Deseja realmente zerar todos os dados do percurso atual (distância, tempo, médias e combustível)?")
                .setTitle("Zerar Viagem Atual?")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Sim, Zerar")
                        .setOnClickListener {
                            TelemetryBridge.requestResetTrip()
                            showConfirmResetDialog = false
                            invalidate()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("Cancelar")
                        .setOnClickListener {
                            showConfirmResetDialog = false
                            invalidate()
                        }
                        .build()
                )
                .build()
        }

        val t = latest
        val hours = t.tripDurationSec / 3600
        val mins = (t.tripDurationSec % 3600) / 60
        val secs = t.tripDurationSec % 60
        val timeFormatted = if (hours > 0) String.format("%02d:%02d:%02d h", hours, mins, secs) else String.format("%02d:%02d min", mins, secs)

        val odoFormatted = if (t.odometerKm > 0f) String.format("%,.0f km", t.odometerKm) else "— km"
        val rangeFormatted = if (t.rangeKm > 0) "${t.rangeKm} km" else "— km"
        val avgKmlFormatted = if (t.averageConsumption > 0f) String.format("%.1f km/L", t.averageConsumption) else "— km/L"
        val instKmlFormatted = if (t.instantConsumption > 0f) String.format("%.1f km/L", t.instantConsumption) else "0.0 km/L"
        val fuelLitersFormatted = String.format("%.2f L", t.totalFuelConsumedLiters)

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(String.format("%.1f km", t.tripDistanceKm))
                    .addText("Distância Percorrida na Viagem (Total Odômetro: $odoFormatted)")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Média: ${String.format("%.1f", t.tripAvgSpeed)} km/h  |  Máx: ${t.tripMaxSpeed.toInt()} km/h")
                    .addText("Velocidade do Percurso")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(timeFormatted)
                    .addText("Tempo em Movimento")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Médio: $avgKmlFormatted  |  Inst: $instKmlFormatted")
                    .addText("Consumo de Combustível")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("$fuelLitersFormatted consumidos  |  Autonomia: $rangeFormatted")
                    .addText("Balanço Energético da Viagem")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Zerar Viagem")
                    .setOnClickListener {
                        showConfirmResetDialog = true
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Computador de Bordo")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
