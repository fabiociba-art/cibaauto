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
import com.example.model.VehicleProfile
import com.example.obd.ObdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Painel Geral de Controle (Dashboard) para Android Auto.
 * Exibe visão unificada do carro, métricas operacionais e funções de diagnóstico e varredura da ECU.
 */
class CibaAutoDashboardCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var latest: TelemetryState = TelemetryState()
    private var currentVehicle: VehicleProfile? = null
    private var vehicleInfo: ObdManager.VehicleInfo? = null
    private var vehicleInfoLoading: Boolean = false
    private var hybridScanRunning: Boolean = false
    private var lastUpdateMs: Long = 0L

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                job = scope.launch {
                    launch {
                        TelemetryBridge.telemetry.collectLatest {
                            latest = it
                            val now = System.currentTimeMillis()
                            if (now - lastUpdateMs >= 1000L) {
                                lastUpdateMs = now
                                invalidate()
                            }
                        }
                    }
                    launch {
                        TelemetryBridge.selectedVehicle.collectLatest {
                            currentVehicle = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.vehicleInfo.collectLatest {
                            vehicleInfo = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.vehicleInfoLoading.collectLatest {
                            vehicleInfoLoading = it
                            invalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.hybridScanRunning.collectLatest {
                            hybridScanRunning = it
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
        val v = currentVehicle
        val vehicleTitle = v?.fullName ?: "CIBA AUTO"

        val ecuInfoText = when {
            vehicleInfoLoading -> "Lendo VIN e calibração da ECU..."
            vehicleInfo != null -> "VIN: ${vehicleInfo?.vin ?: "—"} | Calibração: ${vehicleInfo?.calibrationId ?: "OK"}"
            else -> "ECU: ${v?.engine ?: "Conectada"} • Protocolo ISO 15765-4 (CAN 500k)"
        }

        val scanStatus = if (hybridScanRunning) "Varredura Híbrida 7E2 em execução..." else "Sistema Híbrido: Pronto"

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(vehicleTitle)
                    .addText(ecuInfoText)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.speed.toInt()} km/h  |  ${t.rpm} RPM")
                    .addText("Velocidade e Rotação do Motor")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Carga: ${t.engineLoad}%  |  Acelerador (TPS): ${t.throttlePosition}%")
                    .addText("Demanda de Potência do Condutor")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Água: ${t.coolantTemp} °C  |  Tensão Bateria: ${String.format("%.1f", t.batteryVoltage)} V")
                    .addText("Saúde Térmica e Elétrica")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Médio: ${String.format("%.1f", t.averageConsumption)} km/L  |  Autonomia: ${t.rangeKm} km")
                    .addText(scanStatus)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (vehicleInfoLoading) "Lendo ECU..." else "Ler Info ECU")
                    .setOnClickListener {
                        TelemetryBridge.requestReadVehicleInfo()
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (hybridScanRunning) "Varrendo..." else "Scan Híbrido")
                    .setOnClickListener {
                        TelemetryBridge.requestRunHybridScan()
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Painel Geral do Veículo")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
