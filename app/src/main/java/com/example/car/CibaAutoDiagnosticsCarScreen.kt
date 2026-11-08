package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.model.DiagnosticCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tela de Diagnóstico Automotivo e Códigos DTC para Android Auto.
 * Exibe status da luz de anomalia (MIL), códigos de falha armazenados,
 * monitores de prontidão e ação com diálogo para apagar códigos da ECU.
 */
class CibaAutoDiagnosticsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var diagnosticCodes: List<DiagnosticCode> = emptyList()
    private var readinessMonitors: Map<String, String> = emptyMap()
    private var showConfirmClearDialog: Boolean = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    launch {
                        TelemetryBridge.diagnosticCodes.collectLatest { codes ->
                            diagnosticCodes = codes
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.readinessMonitors.collectLatest { monitors ->
                            readinessMonitors = monitors
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
        if (showConfirmClearDialog) {
            return MessageTemplate.Builder("Deseja realmente enviar o comando Mode 04 para apagar todos os códigos DTC da ECU e reiniciar os monitores de prontidão?")
                .setTitle("Apagar Códigos da ECU?")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Confirmar Limpeza")
                        .setOnClickListener {
                            TelemetryBridge.requestClearDtcCodes()
                            showConfirmClearDialog = false
                            invalidate()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("Cancelar")
                        .setOnClickListener {
                            showConfirmClearDialog = false
                            invalidate()
                        }
                        .build()
                )
                .build()
        }

        val hasCodes = diagnosticCodes.isNotEmpty()
        val milText = if (hasCodes) "Luz de Injeção (MIL): ATIVA (${diagnosticCodes.size} falha(s))" else "Luz de Injeção (MIL): Apagada"

        val list = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(milText)
                    .addText(if (hasCodes) "Falhas ativas registradas na central eletrônica" else "Sistema sem falhas ativas na ECU")
                    .build()
            )

        if (hasCodes) {
            diagnosticCodes.forEach { dtc ->
                list.addItem(
                    Row.Builder()
                        .setTitle("${dtc.code} • ${dtc.title}")
                        .addText("${dtc.description} [${dtc.system}]")
                        .build()
                )
            }
        } else {
            list.addItem(
                Row.Builder()
                    .setTitle("Nenhum Código de Falha Ativo")
                    .addText("A ECU não reportou códigos pendentes no Mode 03.")
                    .build()
            )
        }

        // Monitores de Prontidão (Readiness)
        if (readinessMonitors.isNotEmpty()) {
            readinessMonitors.forEach { (monitor, status) ->
                list.addItem(
                    Row.Builder()
                        .setTitle("Monitor: $monitor")
                        .addText("Status OBD-2: $status")
                        .build()
                )
            }
        }

        val templateBuilder = ListTemplate.Builder()
            .setTitle("Diagnóstico & Falhas DTC")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())

        if (hasCodes) {
            templateBuilder.addAction(
                Action.Builder()
                    .setTitle("Limpar DTCs")
                    .setOnClickListener {
                        showConfirmClearDialog = true
                        invalidate()
                    }
                    .build()
            )
        } else {
            templateBuilder.addAction(
                Action.Builder()
                    .setTitle("Atualizar")
                    .setOnClickListener {
                        TelemetryBridge.requestRefreshDiagnostics()
                        invalidate()
                    }
                    .build()
            )
        }

        return templateBuilder.build()
    }
}
