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
 * Tela de Prova de Desempenho e Aceleração 0 - 100 km/h para Android Auto.
 * Detecta automaticamente o início do teste a partir de 0 km/h até atingir 100 km/h.
 */
class CibaAutoPerformanceCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()

    private var isArmed: Boolean = false
    private var isTiming: Boolean = false
    private var startTimeMs: Long = 0L
    private var lastRecordedTimeSec: Float? = null
    private var bestTimeSec: Float = 9.2f
    private var testPeakSpeed: Float = 0f

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    TelemetryBridge.telemetry.collectLatest { state ->
                        latest = state
                        processTelemetryForTimer(state.speed)
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

    private fun processTelemetryForTimer(speed: Float) {
        val now = System.currentTimeMillis()

        if (speed > testPeakSpeed) {
            testPeakSpeed = speed
        }

        if (isArmed && !isTiming) {
            if (speed > 1.5f) {
                isTiming = true
                startTimeMs = now
                invalidate()
            }
        } else if (isTiming) {
            if (speed >= 100f) {
                val elapsedSec = (now - startTimeMs) / 1000f
                lastRecordedTimeSec = elapsedSec
                if (elapsedSec < bestTimeSec) {
                    bestTimeSec = elapsedSec
                }
                isTiming = false
                isArmed = false
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val t = latest
        val currentSpeed = t.speed.toInt()

        val statusText = when {
            isTiming -> {
                val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000f
                String.format("ACELERANDO! %.2fs (%d km/h)", elapsed, currentSpeed)
            }
            isArmed -> "ARMADO! Pare o carro e pise fundo."
            lastRecordedTimeSec != null -> String.format("Último Registro: %.2f segundos", lastRecordedTimeSec)
            else -> "Aguardando início do teste..."
        }

        val bestFormatted = String.format("%.2f s", bestTimeSec)
        val currentTimerText = if (isTiming) {
            val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000f
            String.format("%.2f s", elapsed)
        } else if (lastRecordedTimeSec != null) {
            String.format("%.2f s", lastRecordedTimeSec)
        } else {
            "—"
        }

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("Tempo 0 - 100 km/h: $currentTimerText")
                    .addText(statusText)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Melhor Marca Registrada: $bestFormatted")
                    .addText("Referência de fábrica do veículo")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Velocidade Atual: $currentSpeed km/h  |  RPM: ${t.rpm}")
                    .addText("Pico na Prova: ${testPeakSpeed.toInt()} km/h")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Segurança: Realize testes apenas em pistas fechadas e seguras")
                    .addText("Respeite as leis de trânsito locais")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isArmed || isTiming) "Cancelar Medição" else "Armar Novo Teste")
                    .setOnClickListener {
                        if (isArmed || isTiming) {
                            isArmed = false
                            isTiming = false
                        } else {
                            isArmed = true
                            isTiming = false
                            testPeakSpeed = 0f
                        }
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Cronômetro de Desempenho")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
