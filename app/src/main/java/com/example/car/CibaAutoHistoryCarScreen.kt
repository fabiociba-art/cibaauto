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
import com.example.model.TripRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Histórico de Viagens e Trajetos Salvos para Android Auto.
 */
class CibaAutoHistoryCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var tripsList: List<TripRecord> = emptyList()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    TelemetryBridge.trips.collectLatest { trips ->
                        tripsList = trips
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
        val list = ItemList.Builder()

        if (tripsList.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle("Nenhum Histórico de Viagem Salvo")
                    .addText("As viagens concluídas e gravadas no smartphone serão exibidas aqui.")
                    .build()
            )
            list.addItem(
                Row.Builder()
                    .setTitle("Gravação Automática Ativa")
                    .addText("O app calcula distância e consumo em segundo plano via OBD-2.")
                    .build()
            )
        } else {
            tripsList.take(6).forEach { trip ->
                list.addItem(
                    Row.Builder()
                        .setTitle("${trip.date} • ${String.format("%.1f km", trip.distanceKm)}")
                        .addText("${trip.durationMinutes} min  |  Méd: ${String.format("%.1f", trip.avgSpeed)} km/h  |  ${String.format("%.1f km/L", trip.avgConsumption)}")
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Histórico de Viagens")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }
}
