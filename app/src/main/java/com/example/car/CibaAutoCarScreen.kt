package com.example.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.R
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * Tela principal do CIBA AUTO para o Android Auto.
 * Disponibiliza 100% das telas e funções do smartphone através de um TabTemplate nativo
 * e navegação completa por telas filhas (Stack Navigation) da Car App Library:
 *
 * 1. Aba "Cluster": Cockpit esportivo (Velocidade grande, RPM, Marcha estimada, Autonomia, Bateria).
 * 2. Aba "Energia": Fluxo de Energia Híbrido com o carro vetorial desenhado, SOC da bateria HV e kW.
 * 3. Aba "Painel": Dashboard geral com dados da ECU, métricas do motor e varredura híbrida.
 * 4. Aba "Telas": Menu completo com acesso a todas as telas especializadas:
 *    - Sensores OBD-2 (Todos os PIDs)
 *    - Monitor Flex Etanol
 *    - Diagnóstico & Limpeza de DTCs
 *    - Computador de Bordo & Viagem
 *    - Cronômetro de Desempenho 0-100 km/h
 *    - Histórico de Viagens
 *    - Perfis de Veículos Toyota
 *    - Ajustes & Conexão Bluetooth
 */
class CibaAutoCarScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null

    private var latest: TelemetryState = TelemetryState()
    private var vehicleName: String = "CIBA AUTO"
    private var isPowerInverted: Boolean = false
    private var activeAlerts: List<CarAlert> = emptyList()

    // Controle de abas
    private var currentTabId: String = TAB_CLUSTER
    private var showOfflineTabs: Boolean = false

    // Controle de throttling de atualização (evita ultrapassar limites de taxa do Android Auto)
    private var lastInvalidateTimestampMs: Long = 0L
    private var lastRenderedMode: EnergyMode? = null
    private var lastRenderedSpeed: Int = -1
    private var lastRenderedRpm: Int = -1
    private var lastRenderedSoc: Int = -1

    companion object {
        const val TAB_CLUSTER = "tab_cluster"
        const val TAB_ENERGY = "tab_energy"
        const val TAB_DASHBOARD = "tab_dashboard"
        const val TAB_MENU = "tab_menu"
        const val MIN_UPDATE_INTERVAL_MS = 1000L // 1 segundo
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                collectJob = scope.launch {
                    launch {
                        TelemetryBridge.telemetry.collectLatest { state ->
                            latest = state
                            checkAndInvalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.vehicleName.collectLatest { name ->
                            vehicleName = name
                            checkAndInvalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.isHybridPowerInverted.collectLatest { inv ->
                            isPowerInverted = inv
                            checkAndInvalidate()
                        }
                    }
                    launch {
                        TelemetryBridge.alerts.collectLatest { alerts ->
                            activeAlerts = alerts
                            checkAndInvalidate()
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

    /**
     * Limita as invocações de invalidate() para no máximo 1x por segundo e
     * apenas quando houver alterações significativas nos valores visíveis.
     */
    private fun checkAndInvalidate() {
        val now = System.currentTimeMillis()
        if (now - lastInvalidateTimestampMs < MIN_UPDATE_INTERVAL_MS) {
            return
        }

        val t = latest
        val mode = deriveEnergyMode(t, isHybridVehicle = true, invertKwSign = isPowerInverted)
        val curSpeed = t.speed.toInt()
        val curRpm = t.rpm
        val curSoc = t.hybridBatterySoc

        val hasChanged = mode != lastRenderedMode ||
                abs(curSpeed - lastRenderedSpeed) >= 2 ||
                abs(curRpm - lastRenderedRpm) >= 100 ||
                curSoc != lastRenderedSoc ||
                activeAlerts.isNotEmpty()

        if (hasChanged) {
            lastInvalidateTimestampMs = now
            lastRenderedMode = mode
            lastRenderedSpeed = curSpeed
            lastRenderedRpm = curRpm
            lastRenderedSoc = curSoc
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val t = latest

        // Se estiver desconectado e o usuário ainda não escolheu ver abas offline, exibe MessageTemplate com atalhos
        if (!t.isConnected && !t.isDemoMode && !showOfflineTabs) {
            return buildDisconnectedTemplate()
        }

        // Compatibilidade total: se a central multimídia estiver em Car API Level < 6 (sem suporte a TabTemplate),
        // entrega o template correspondente diretamente para evitar falha no host
        if (carContext.carAppApiLevel < 6) {
            return when (currentTabId) {
                TAB_ENERGY -> buildEnergyTabTemplate()
                TAB_DASHBOARD -> buildDashboardTabTemplate()
                TAB_MENU -> buildMenuTabTemplate()
                else -> buildClusterTabTemplate()
            }
        }

        val tabCluster = Tab.Builder()
            .setTitle("Cluster")
            .setContentId(TAB_CLUSTER)
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_cluster)).build())
            .build()

        val tabEnergy = Tab.Builder()
            .setTitle("Energia")
            .setContentId(TAB_ENERGY)
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_energy)).build())
            .build()

        val tabDashboard = Tab.Builder()
            .setTitle("Painel")
            .setContentId(TAB_DASHBOARD)
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_dashboard)).build())
            .build()

        val tabMenu = Tab.Builder()
            .setTitle("Telas")
            .setContentId(TAB_MENU)
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_menu)).build())
            .build()

        val embeddedTemplate = when (currentTabId) {
            TAB_CLUSTER -> buildClusterTabTemplate()
            TAB_ENERGY -> buildEnergyTabTemplate()
            TAB_DASHBOARD -> buildDashboardTabTemplate()
            TAB_MENU -> buildMenuTabTemplate()
            else -> buildClusterTabTemplate()
        }

        return TabTemplate.Builder(object : TabTemplate.TabCallback {
            override fun onTabSelected(tabContentId: String) {
                currentTabId = tabContentId
                invalidate()
            }
        })
            .addTab(tabCluster)
            .addTab(tabEnergy)
            .addTab(tabDashboard)
            .addTab(tabMenu)
            .setActiveTabContentId(currentTabId)
            .setTabContents(TabContents.Builder(embeddedTemplate).build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    /**
     * Tela de desconexão com ações diretas para o motorista.
     */
    private fun buildDisconnectedTemplate(): Template {
        return MessageTemplate.Builder("Conecte o adaptador ELM327 na tomada OBD-2 do carro ou inicie o Modo Demonstração para testar todas as funções.")
            .setTitle("CIBA AUTO • OBD-2 Desconectado")
            .setIcon(CarIcon.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle("Conectar")
                    .setOnClickListener {
                        TelemetryBridge.requestConnect()
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Modo Demo")
                    .setOnClickListener {
                        TelemetryBridge.requestToggleDemo()
                        invalidate()
                    }
                    .build()
            )
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Ver Telas")
                            .setOnClickListener {
                                showOfflineTabs = true
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * 1. Aba "Cluster": Cockpit esportivo para direção (Velocidade, RPM, marcha, autonomia, bateria).
     */
    private fun buildClusterTabTemplate(): Template {
        val t = latest
        val speed = t.speed.toInt()
        val rpm = t.rpm

        val gearEstimate = when {
            speed == 0 && rpm == 0 -> "P"
            speed == 0 -> "N"
            t.hybridMotorPowerKw < -1.5f -> "B (Regen)"
            else -> "D"
        }

        val rangeText = if (t.rangeKm > 0) "${t.rangeKm} km" else if (t.isConnected) "Calculando..." else "— km"
        val odoText = if (t.odometerKm > 0f) String.format("%,.0f km", t.odometerKm) else "— km"

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("$speed km/h")
                    .addText("Velocidade Instantânea  •  Marcha: $gearEstimate")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("$rpm RPM")
                    .addText("Rotação do Motor a Combustão")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Autonomia: $rangeText  |  Odômetro: $odoText")
                    .addText(vehicleName)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Bateria 12V: ${String.format("%.1f", t.batteryVoltage)} V  |  Água: ${t.coolantTemp} °C")
                    .addText("Status Elétrico e Arrefecimento")
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
            .addAction(
                Action.Builder()
                    .setTitle("Diagnóstico")
                    .setOnClickListener {
                        screenManager.push(CibaAutoDiagnosticsCarScreen(carContext))
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Cluster Digital")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    /**
     * 2. Aba "Energia": Fluxo de Energia Híbrido, diagrama visual em bitmap, SOC e kW.
     */
    private fun buildEnergyTabTemplate(): Template {
        val t = latest
        val mode = deriveEnergyMode(t, isHybridVehicle = true, invertKwSign = isPowerInverted)
        val effectiveKw = if (isPowerInverted) -t.hybridMotorPowerKw else t.hybridMotorPowerKw

        val modeTitle = when (mode) {
            EnergyMode.EV -> "Modo 100% Elétrico (EV)"
            EnergyMode.CHARGING -> "Regeneração de Energia"
            EnergyMode.HYBRID -> "Modo Híbrido Combinado"
            EnergyMode.ENGINE -> "Modo Combustão Ativo"
            EnergyMode.STANDBY -> "Sistema em Espera (READY)"
        }

        val flowDescription = when (mode) {
            EnergyMode.EV -> "⚡ Bateria HV ➔ Motor Elétrico ➔ Rodas"
            EnergyMode.CHARGING -> "🔋 Rodas (Frenagem) ➔ Gerador ➔ Bateria"
            EnergyMode.HYBRID -> "⚡🔥 Combustão + Bateria ➔ Rodas Dianteiras"
            EnergyMode.ENGINE -> "🔥 Motor a Combustão ➔ Rodas Dianteiras"
            EnergyMode.STANDBY -> "Standby: Carro Parado ou Desligado"
        }

        val soc = t.hybridBatterySoc
        val socText = if (soc > 0) "$soc%" else "—"
        val socSubtitle = when {
            soc in 1..19 -> "Bateria HV Carga Baixa"
            soc in 20..50 -> "Bateria HV Carga Média"
            soc > 50 -> "Bateria HV Carga Ideal"
            else -> "Aguardando leitura ECU 7E2"
        }

        val kwFormatted = String.format("%+.1f kW", effectiveKw)
        val kwSubtitle = if (effectiveKw < -0.8f) "Recarga / Regeneração" else if (effectiveKw > 0.8f) "Tração Elétrica" else "Neutro"

        val diagramIcon = CarEnergyFlowBitmapGenerator.getCarIconForMode(mode, isHybrid = true)

        val pane = Pane.Builder()
            .setImage(diagramIcon)
            .addRow(
                Row.Builder()
                    .setTitle(modeTitle)
                    .addText(flowDescription)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(kwFormatted)
                    .addText("Potência Elétrica: $kwSubtitle")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("SOC Bateria HV: $socText")
                    .addText(socSubtitle)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.speed.toInt()} km/h  |  ${t.rpm} RPM")
                    .addText("Regime Dinâmico Atual")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (t.isDemoMode) "Parar Demo" else "Modo Demo")
                    .setOnClickListener {
                        TelemetryBridge.requestToggleDemo()
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isPowerInverted) "kW Invertido" else "Inverter kW")
                    .setOnClickListener {
                        TelemetryBridge.requestTogglePowerInvert()
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Fluxo de Energia")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    /**
     * 3. Aba "Painel": Dashboard geral com dados do motor, ECU, consumo e atalhos rápidos.
     */
    private fun buildDashboardTabTemplate(): Template {
        val t = latest

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(vehicleName)
                    .addText("Perfil Selecionado no CIBA AUTO")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("${t.speed.toInt()} km/h  |  ${t.rpm} RPM")
                    .addText("Carga: ${t.engineLoad}%  |  Acelerador (TPS): ${t.throttlePosition}%")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Consumo Médio: ${String.format("%.1f", t.averageConsumption)} km/L")
                    .addText("Instantâneo: ${String.format("%.1f", t.instantConsumption)} km/L  |  Vazão: ${String.format("%.1f", t.fuelFlowLitersPerHour)} L/h")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Água: ${t.coolantTemp} °C  |  Admissão IAT: ${t.intakeTemp} °C")
                    .addText("MAP: ${t.manifoldPressure.toInt()} kPa  |  Autonomia: ${t.rangeKm} km")
                    .addText("Etanol: ${t.ethanolPercentage}% (${t.fuelTypeLabel})")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Computador Viagem")
                    .setOnClickListener {
                        screenManager.push(CibaAutoTripCarScreen(carContext))
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Desempenho 0-100")
                    .setOnClickListener {
                        screenManager.push(CibaAutoPerformanceCarScreen(carContext))
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Painel de Telemetria")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    /**
     * 4. Aba "Telas": Menu completo com navegação para todas as 11 telas e funções do app.
     */
    private fun buildMenuTabTemplate(): Template {
        val list = buildMenuItemList(carContext, screenManager, 0)

        return ListTemplate.Builder()
            .setTitle("Todas as Telas & Funções")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .build()
    }
}

/** Uma entrada do menu de telas do carro. */
private class CarMenuEntry(
    val title: String,
    val text: String,
    val icon: Int,
    val open: (CarContext) -> Screen
)

private fun carMenuEntries(): List<CarMenuEntry> = listOf(
    CarMenuEntry("1. Sensores OBD-2 (Todos os PIDs)", "RPM, velocidade, arrefecimento, ar, carga, TPS, MAP, tensão e fluxo", R.drawable.ic_car_sensor) { CibaAutoSensorsCarScreen(it) },
    CarMenuEntry("2. Monitor de Combustível Flex", "Teor de Etanol, autonomia comparativa e análise econômica dos 70%", R.drawable.ic_car_ethanol) { CibaAutoEthanolCarScreen(it) },
    CarMenuEntry("3. Diagnóstico Automotivo & DTCs", "Leitura de falhas ativas, luz MIL, prontidão e comando de apagar falhas", R.drawable.ic_car_diag) { CibaAutoDiagnosticsCarScreen(it) },
    CarMenuEntry("4. Computador de Bordo & Viagem", "Distância, velocidade média/máxima, tempo de viagem, consumo e zerar percurso", R.drawable.ic_car_trip) { CibaAutoTripCarScreen(it) },
    CarMenuEntry("5. Desempenho 0 - 100 km/h", "Cronômetro de aceleração esportivo e medição automática por telemetria", R.drawable.ic_car_performance) { CibaAutoPerformanceCarScreen(it) },
    CarMenuEntry("6. Painel Detalhado da ECU", "Informações do veículo, VIN, calibração e varredura do sistema híbrido", R.drawable.ic_car_dashboard) { CibaAutoDashboardCarScreen(it) },
    CarMenuEntry("7. Histórico de Viagens", "Consulta de trajetos gravados com estatísticas de consumo", R.drawable.ic_car_history) { CibaAutoHistoryCarScreen(it) },
    CarMenuEntry("8. Perfis de Veículos (Toyota)", "Visualizar e alternar o perfil ativo do carro diretamente na multimídia", R.drawable.ic_car_vehicle) { CibaAutoVehiclesCarScreen(it) },
    CarMenuEntry("9. Ajustes & Conexão Bluetooth", "Conectar/desconectar ELM327, selecionar dispositivo pareado e modo demo", R.drawable.ic_car_settings) { CibaAutoSettingsCarScreen(it) }
)

/**
 * Monta a lista do menu a partir de [startIndex], respeitando o limite de itens que o carro permite.
 * Se não couber tudo, o último item vira "Mais telas" e abre a próxima página.
 */
private fun buildMenuItemList(
    carContext: CarContext,
    screenManager: androidx.car.app.ScreenManager,
    startIndex: Int
): ItemList {
    val limit = carListLimit(carContext)
    val remaining = carMenuEntries().drop(startIndex)
    val needsMore = remaining.size > limit
    val shown = if (needsMore) remaining.take(limit - 1) else remaining

    val builder = ItemList.Builder()
    shown.forEach { e ->
        builder.addItem(
            Row.Builder()
                .setTitle(e.title)
                .addText(e.text)
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, e.icon)).build())
                .setOnClickListener { screenManager.push(e.open(carContext)) }
                .build()
        )
    }
    if (needsMore) {
        builder.addItem(
            Row.Builder()
                .setTitle("Mais telas ›")
                .addText("Ver as demais telas e funções")
                .setBrowsable(true)
                .setOnClickListener {
                    screenManager.push(CibaAutoMenuMoreCarScreen(carContext, startIndex + shown.size))
                }
                .build()
        )
    }
    return builder.build()
}

/** Página extra do menu, usada quando o carro limita a quantidade de itens da lista. */
class CibaAutoMenuMoreCarScreen(carContext: CarContext, private val startIndex: Int) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return ListTemplate.Builder()
            .setTitle("Mais Telas & Funções")
            .setHeaderAction(Action.BACK)
            .setSingleList(buildMenuItemList(carContext, screenManager, startIndex))
            .build()
    }
}
