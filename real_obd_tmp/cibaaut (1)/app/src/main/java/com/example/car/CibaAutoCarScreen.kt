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
 * Tela principal no Android Auto.
 * Exibe telemetria OBD em templates oficiais (Pane + Grid).
 * Atualiza automaticamente quando o TelemetryBridge muda.
 */
class CibaAutoCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null
    private var latest: TelemetryState = TelemetryState()
    private var vehicleName: String = "CIBA AUTO"

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                collectJob = scope.launch {
                    launch {
                        TelemetryBridge.telemetry.collectLatest { state ->
                            latest = state
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.vehicleName.collectLatest { name ->
                            vehicleName = name
                            invalidate()
                        }
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                collectJob?.cancel()
                collectJob = null
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val t = latest
        val status = when {
            t.isConnected -> "OBD ONLINE"
            else -> "DESCONECTADO"
        }

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("${t.speed.toInt()} km/h")
                    .addText("Velocidade")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.rpm} RPM")
                    .addText("Rotação do motor")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(String.format("%.1f V", t.batteryVoltage))
                    .addText("Tensão da bateria")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.ethanolPercentage}%")
                    .addText("Etanol no tanque")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.coolantTemp} °C")
                    .addText("Temperatura da água")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.engineLoad}%  |  TPS ${t.throttlePosition}%")
                    .addText("Carga / Acelerador")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.manifoldPressure.toInt()} kPa  |  IAT ${t.intakeTemp} °C")
                    .addText("MAP / Admissão")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(status)
                    .addText(vehicleName)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Sensores")
                    .setOnClickListener {
                        screenManager.push(CibaAutoSensorsCarScreen(carContext))
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("CIBA AUTO")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
