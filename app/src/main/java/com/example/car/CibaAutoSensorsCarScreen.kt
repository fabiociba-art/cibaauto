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
import kotlin.math.abs

/**
 * Tela de todos os sensores OBD-2 em tempo real para Android Auto.
 * Exibe lista completa de PIDs padronizados e avançados Toyota com atualização controlada.
 */
class CibaAutoSensorsCarScreen(carContext: CarContext) : Screen(carContext) {

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
        val allRows = listOf(
            row("010C  RPM (Rotação do Motor)", "${t.rpm} RPM"),
            row("010D  Velocidade do Veículo", "${t.speed.toInt()} km/h"),
            row("0105  Temperatura da Água (Coolant)", "${t.coolantTemp} °C"),
            row("010F  Temperatura do Ar (IAT)", "${t.intakeTemp} °C"),
            row("0104  Carga Calculada do Motor", "${t.engineLoad} %"),
            row("0111  Posição do Acelerador (TPS)", "${t.throttlePosition} %"),
            row("010B  Pressão no Coletor (MAP)", "${t.manifoldPressure.toInt()} kPa"),
            row("0142  Tensão da Bateria 12V", String.format("%.1f V", t.batteryVoltage)),
            row("0152  Teor de Etanol (Flex Fuel)", "${t.ethanolPercentage} % (${t.fuelTypeLabel})"),
            row("010A  Pressão na Linha de Combustível", "${t.fuelPressure.toInt()} kPa"),
            row("CONSUMO  Vazão Instantânea", String.format("%.1f L/h", t.fuelFlowLitersPerHour)),
            row("BATERIA HV  Estado de Carga (SOC)", if (t.hybridBatterySoc > 0) "${t.hybridBatterySoc} %" else "—"),
            row("MOTOR ELÉTRICO  Potência Instantânea", String.format("%+.1f kW", t.hybridMotorPowerKw))
        )
        val list = ItemList.Builder()
            .apply { allRows.take(carListLimit(carContext)).forEach { addItem(it) } }
            .build()

        return ListTemplate.Builder()
            .setTitle("Sensores OBD-2 em Tempo Real")
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
