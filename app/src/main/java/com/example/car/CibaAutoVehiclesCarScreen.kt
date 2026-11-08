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
import com.example.model.VehicleProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tela de Seleção e Perfis de Veículos para Android Auto.
 * Permite ao motorista alternar entre os perfis do carro diretamente na multimídia.
 */
class CibaAutoVehiclesCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var vehicleList: List<VehicleProfile> = emptyList()
    private var selectedVehicleId: String? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    launch {
                        TelemetryBridge.vehicles.collectLatest { list ->
                            vehicleList = list
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.selectedVehicle.collectLatest { sel ->
                            selectedVehicleId = sel.id
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
        val list = ItemList.Builder()

        vehicleList.forEach { profile ->
            val isSelected = profile.id == selectedVehicleId
            val prefix = if (isSelected) "✓ ATIVO: " else ""
            val hybridTag = if (profile.isHybrid) " • Híbrido Flex" else ""

            list.addItem(
                Row.Builder()
                    .setTitle("$prefix${profile.fullName}")
                    .addText("${profile.engine} • ${profile.transmission}$hybridTag")
                    .setOnClickListener {
                        TelemetryBridge.requestSelectVehicle(profile)
                        invalidate()
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Perfis de Veículos")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }
}
