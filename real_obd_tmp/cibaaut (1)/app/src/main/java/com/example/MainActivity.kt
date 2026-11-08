package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.PermissionsGate
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CibaAutoViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
  object Dashboard : Screen("dashboard", "Painel", Icons.Default.Dashboard)
  object Ethanol : Screen("ethanol", "Etanol", Icons.Default.LocalGasStation)
  object Diagnostics : Screen("diagnostics", "Diagnóstico", Icons.Default.Build)
  object Sensors : Screen("sensors", "Sensores", Icons.Default.Memory)
  object Charts : Screen("charts", "Gráficos", Icons.Default.ShowChart)
  object Performance : Screen("performance", "Desempenho", Icons.Default.Timer)
  object Trip : Screen("trip", "Viagem", Icons.Default.Navigation)
  object History : Screen("history", "Histórico", Icons.Default.History)
  object Vehicles : Screen("vehicles", "Veículos", Icons.Default.DirectionsCar)
  object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
  object Cluster : Screen("cluster", "Cluster", Icons.Default.Speed)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        PermissionsGate {
        val viewModel: CibaAutoViewModel = viewModel()
        val vehicle by viewModel.selectedVehicle.collectAsState()
        val telemetry by viewModel.telemetry.collectAsState()
        val diagnosticCodes by viewModel.diagnosticCodes.collectAsState()
        val readinessMonitors by viewModel.readinessMonitors.collectAsState()
        val trips by viewModel.trips.collectAsState()
        val ethanolHistory by viewModel.ethanolHistory.collectAsState()
        val connectionLogs by viewModel.connectionLogs.collectAsState()
        val btDevices by viewModel.btDevices.collectAsState()
        val selectedBtAddress by viewModel.selectedBtAddress.collectAsState()
        val btEnabled by viewModel.btEnabled.collectAsState()

        var showSplash by remember { mutableStateOf(true) }
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Cluster) }

        if (showSplash) {
          SplashScreen(onSplashFinished = { showSplash = false })
        } else {
          Scaffold(
              modifier = Modifier.fillMaxSize(),
              bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                  val primaryItems = listOf(
                      Screen.Cluster,
                      Screen.Dashboard,
                      Screen.Ethanol,
                      Screen.Diagnostics,
                      Screen.Settings
                  )
                  primaryItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                  }
                }
              }
          ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
              when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(
                    vehicle = vehicle,
                    telemetry = telemetry,
                    onNavigateToEthanol = { currentScreen = Screen.Ethanol },
                    onNavigateToVehicles = { currentScreen = Screen.Vehicles },
                    onToggleConnection = {
                      if (telemetry.isConnected) {
                        viewModel.disconnectObd()
                      } else {
                        viewModel.connectObd()
                      }
                    }
                )
                is Screen.Ethanol -> EthanolScreen(
                    telemetry = telemetry,
                    ethanolHistory = ethanolHistory
                )
                is Screen.Diagnostics -> DiagnosticsScreen(
                    diagnosticCodes = diagnosticCodes,
                    readinessMonitors = readinessMonitors,
                    onClearCodes = { viewModel.clearDtcCodes() }
                )
                is Screen.Sensors -> SensorsScreen(telemetry = telemetry)
                is Screen.Charts -> ChartsScreen(telemetry = telemetry)
                is Screen.Performance -> PerformanceScreen()
                is Screen.Trip -> TripScreen()
                is Screen.History -> HistoryScreen(trips = trips)
                is Screen.Vehicles -> VehiclesScreen(
                    selectedVehicle = vehicle,
                    onSelectVehicle = { selected ->
                      viewModel.selectVehicle(selected)
                      currentScreen = Screen.Dashboard
                    }
                )
                is Screen.Cluster -> AndroidAutoClusterScreen(telemetry = telemetry)
                is Screen.Settings -> SettingsScreen(
                    telemetry = telemetry,
                    connectionLogs = connectionLogs,
                    btDevices = btDevices,
                    selectedBtAddress = selectedBtAddress,
                    btEnabled = btEnabled,
                    onRefreshDevices = { viewModel.refreshBtDevices() },
                    onSelectDevice = { address -> viewModel.selectBtDevice(address) },
                    onConnect = { viewModel.connectObd() },
                    onDisconnect = { viewModel.disconnectObd() }
                )
              }
            }
          }
        }
        } // PermissionsGate
      }
    }
  }
}
