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
import com.example.obd.ObdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tela de Ajustes, Comunicação e Conexão Bluetooth para Android Auto.
 * Permite pareamento, seleção do adaptador OBD-2, alternância de modo demo e inversão do sinal de potência.
 */
class CibaAutoSettingsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()
    private var devicesList: List<ObdManager.BtDeviceInfo> = emptyList()
    private var selectedAddress: String? = null
    private var isPowerInverted: Boolean = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    launch {
                        TelemetryBridge.telemetry.collectLatest {
                            latest = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.btDevices.collectLatest {
                            devicesList = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.selectedBtAddress.collectLatest {
                            selectedAddress = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.isHybridPowerInverted.collectLatest {
                            isPowerInverted = it
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
        val list = ItemList.Builder()

        // Estado da Conexão
        val connTitle = when {
            t.isConnected && !t.isDemoMode -> "OBD-2 Conectado ao Veículo"
            t.isDemoMode -> "Modo Demonstração (Simulação Ativa)"
            else -> "OBD-2 Desconectado"
        }
        val connActionText = if (t.isConnected) "Toque para Desconectar" else "Toque para Conectar"

        list.addItem(
            Row.Builder()
                .setTitle(connTitle)
                .addText(connActionText)
                .setOnClickListener {
                    if (t.isConnected) {
                        TelemetryBridge.requestDisconnect()
                    } else {
                        TelemetryBridge.requestConnect()
                    }
                    invalidate()
                }
                .build()
        )

        // Alternar Modo Demonstração
        list.addItem(
            Row.Builder()
                .setTitle(if (t.isDemoMode) "Parar Modo Demonstração" else "Iniciar Modo Demonstração")
                .addText("Simula o ciclo híbrido completo (Standby, EV, Combustão, Híbrido, Regeneração)")
                .setOnClickListener {
                    TelemetryBridge.requestToggleDemo()
                    invalidate()
                }
                .build()
        )

        // Alternar Inversão do Sinal da Potência Híbrida
        list.addItem(
            Row.Builder()
                .setTitle(if (isPowerInverted) "Sinal Potência kW: Invertido" else "Sinal Potência kW: Padrão")
                .addText("Inverte a polaridade (+ descarga / - recarga) para compatibilidade Toyota")
                .setOnClickListener {
                    TelemetryBridge.requestTogglePowerInvert()
                    invalidate()
                }
                .build()
        )

        // Lista de Adaptadores Bluetooth
        if (devicesList.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle("Buscar Dispositivos Bluetooth")
                    .addText("Toque para atualizar a lista de adaptadores pareados")
                    .setOnClickListener {
                        TelemetryBridge.requestRefreshBtDevices()
                        invalidate()
                    }
                    .build()
            )
        } else {
            devicesList.take((carListLimit(carContext) - 3).coerceAtLeast(1)).forEach { dev ->
                val isSelected = dev.address == selectedAddress
                val prefix = if (isSelected) "✓ CONECTAR A: " else ""
                val obdHint = if (dev.isLikelyObd) " • Adaptador OBD Reconhecido" else ""

                list.addItem(
                    Row.Builder()
                        .setTitle("$prefix${dev.name}")
                        .addText("${dev.address}$obdHint")
                        .setOnClickListener {
                            TelemetryBridge.requestSelectBtDevice(dev.address)
                            invalidate()
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle("Ajustes & Conexão Bluetooth")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }
}
