package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ObdConnectionState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DriveMode
import com.example.ui.components.CockpitBottomBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HybridEnergyFlowScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CibaAutoTheme
import com.example.ui.theme.CockpitBlack
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CibaAutoViewModel

class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: CibaAutoViewModel by viewModels()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    // Android 12+: permissão "Dispositivos próximos" pedida logo na abertura, para conectar ao OBD sozinho
    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.onBluetoothPermissionGranted()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Mantém a tela ligada enquanto o painel está aberto (leitura contínua no carro)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable fullscreen immersive mode (hide status bar per user requirement)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        // Setup real accelerometer sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }

        setContent {
            CibaAutoTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            viewModel.updateAccelerometer(x)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun MainAppContent(viewModel: CibaAutoViewModel) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val gaugeSlots by viewModel.gaugeSlots.collectAsStateWithLifecycle()
    val connectingAddress by viewModel.connectingAddress.collectAsStateWithLifecycle()
    val liveDataReceived by viewModel.liveDataReceived.collectAsStateWithLifecycle()

    // Faixa de status da ECU (só no modo real, com o adaptador conectado)
    val ecuBannerText: String? = when {
        isDemoMode || connectionState != ObdConnectionState.CONNECTED -> null
        !liveDataReceived -> "Conectado - iniciando leitura da ECU..."
        !telemetry.ecuLinkOk -> "Sem comunicação com a ECU — valores indisponíveis"
        else -> null
    }
    val ecuBannerColor = if (!liveDataReceived) Color(0xCC1E3A5F) else Color(0xCCB91C1C)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBlack),
        containerColor = CockpitBlack,
        bottomBar = {
            CockpitBottomBar(
                currentScreen = currentScreen,
                connectionState = connectionState,
                isDemoMode = isDemoMode,
                driveMode = telemetry.driveMode,
                isHybridVehicle = selectedProfile.isHybrid,
                hasFaults = diagnostics.hasFaults,
                onScreenSelected = { viewModel.selectScreen(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // A faixa fica acima do conteúdo (não por cima do título das telas)
            if (ecuBannerText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ecuBannerColor)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = ecuBannerText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        // Com a faixa visível ela já ocupa a barra de status: as telas não repetem esse espaço
                        if (ecuBannerText != null) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier
                    )
            ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            telemetry = telemetry,
                            profile = selectedProfile,
                            onOpenSettings = { viewModel.selectScreen(AppScreen.SETTINGS) },
                            diagnostics = diagnostics,
                            onOpenDiagnostics = { viewModel.selectScreen(AppScreen.DIAGNOSTICS) },
                            gaugeSlots = gaugeSlots,
                            onGaugeSlotChanged = { slot, id -> viewModel.setGaugeSlot(slot, id) },
                            onResetGauges = { viewModel.resetGaugeSlots() }
                        )
                    }

                    AppScreen.DIAGNOSTICS -> {
                        DiagnosticsScreen(
                            diagnostics = diagnostics,
                            telemetry = telemetry,
                            profile = selectedProfile,
                            connectionState = connectionState,
                            isDemoMode = isDemoMode,
                            onScan = { viewModel.scanDtcs() },
                            onClear = { viewModel.clearDtcs() },
                            onBack = { viewModel.selectScreen(AppScreen.DASHBOARD) }
                        )
                    }

                    AppScreen.HYBRID_FLOW -> {
                        HybridEnergyFlowScreen(
                            telemetry = telemetry,
                            profile = selectedProfile,
                            onOpenSettings = { viewModel.selectScreen(AppScreen.SETTINGS) },
                            onBack = { viewModel.selectScreen(AppScreen.DASHBOARD) }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            selectedProfile = selectedProfile,
                            allProfiles = allProfiles,
                            isDemoMode = isDemoMode,
                            connectionState = connectionState,
                            connectedDevice = connectedDevice,
                            pairedDevices = pairedDevices,
                            diagnosticLogs = diagnosticLogs,
                            onProfileSelected = { viewModel.selectProfile(it) },
                            onProfileUpdated = { viewModel.updateProfile(it) },
                            onDemoModeToggled = { viewModel.setDemoMode(it) },
                            onConnectDevice = { viewModel.connectDevice(it) },
                            onDisconnectDevice = { viewModel.disconnectDevice() },
                            onRefreshDevices = { viewModel.refreshDevices() },
                            onBack = { viewModel.selectScreen(AppScreen.DASHBOARD) },
                            connectingAddress = connectingAddress
                        )
                    }
                }
            }
            }
        }
    }
}
