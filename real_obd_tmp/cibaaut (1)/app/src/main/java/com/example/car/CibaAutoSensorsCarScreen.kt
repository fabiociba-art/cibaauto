package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
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

class CibaAutoSensorsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    TelemetryBridge.telemetry.collectLatest {
                        latest = it
                        invalidate()
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
        val list = ItemList.Builder()
            .addItem(row("010C  RPM", "${t.rpm} RPM"))
            .addItem(row("010D  Velocidade", "${t.speed.toInt()} km/h"))
            .addItem(row("0105  Coolant", "${t.coolantTemp} °C"))
            .addItem(row("010F  IAT", "${t.intakeTemp} °C"))
            .addItem(row("0104  Carga", "${t.engineLoad} %"))
            .addItem(row("0111  TPS", "${t.throttlePosition} %"))
            .addItem(row("010B  MAP", "${t.manifoldPressure.toInt()} kPa"))
            .addItem(row("0142  Bateria", String.format("%.1f V", t.batteryVoltage)))
            .addItem(row("0152  Etanol", "${t.ethanolPercentage} %"))
            .addItem(row("010A  Pressão combustível", "${t.fuelPressure.toInt()} kPa"))
            .build()

        return ListTemplate.Builder()
            .setTitle("Sensores OBD-2")
            .setHeaderAction(Action.BACK)
            .setSingleList(list)
            .build()
    }

    private fun row(title: String, value: String): Row {
        return Row.Builder()
            .setTitle(title)
            .addText(value)
            .build()
    }
}
