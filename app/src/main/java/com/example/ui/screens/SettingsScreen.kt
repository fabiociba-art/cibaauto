package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import com.example.ui.components.ProfileEditDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BluetoothDeviceItem
import com.example.model.DriveMode
import com.example.model.ObdConnectionState
import com.example.model.VehicleProfile
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitCardBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    selectedProfile: VehicleProfile,
    allProfiles: List<VehicleProfile>,
    isDemoMode: Boolean,
    connectionState: ObdConnectionState,
    connectedDevice: BluetoothDeviceItem?,
    pairedDevices: List<BluetoothDeviceItem>,
    diagnosticLogs: List<String>,
    onProfileSelected: (VehicleProfile) -> Unit,
    onProfileUpdated: (VehicleProfile) -> Unit = {},
    onDemoModeToggled: (Boolean) -> Unit,
    onConnectDevice: (BluetoothDeviceItem) -> Unit,
    onDisconnectDevice: () -> Unit,
    onRefreshDevices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    connectingAddress: String? = null
) {
    BackHandler { onBack() }

    var profileQuery by remember { mutableStateOf("") }
    var profilesExpanded by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<VehicleProfile?>(null) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    val brands = remember(allProfiles) { allProfiles.map { it.brand }.distinct().sorted() }
    val filteredProfiles = remember(allProfiles, profileQuery, selectedBrand) {
        val q = profileQuery.trim().lowercase()
        allProfiles.filter { p ->
            (selectedBrand == null || p.brand == selectedBrand) &&
                (q.isEmpty() || p.name.lowercase().contains(q) || p.engine.lowercase().contains(q))
        }
    }

    // Bluetooth permission launcher for Android 12+ (API 31+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            onRefreshDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
            )
        } else {
            onRefreshDevices()
        }
    }

    var showLogs by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_settings_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar ao Dashboard",
                    tint = TextWhite
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "v3.1",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Status Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_connection_status"),
                    colors = CardDefaults.cardColors(containerColor = CockpitCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CockpitCardBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isDemoMode -> GaugeYellow
                                                connectionState == ObdConnectionState.CONNECTED -> GaugeGreen
                                                connectionState == ObdConnectionState.CONNECTING -> GaugeYellow
                                                else -> Color(0xFF6B7280)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Status da Conexão",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }

                            Text(
                                text = when {
                                    isDemoMode -> "Modo Demo Ativo"
                                    connectionState == ObdConnectionState.CONNECTED -> "Conectado"
                                    connectionState == ObdConnectionState.CONNECTING -> "Conectando..."
                                    connectionState == ObdConnectionState.ERROR -> "Erro de Conexão"
                                    else -> "Desconectado"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDemoMode -> GaugeYellow
                                    connectionState == ObdConnectionState.CONNECTED -> GaugeGreen
                                    connectionState == ObdConnectionState.CONNECTING -> GaugeYellow
                                    connectionState == ObdConnectionState.ERROR -> GaugeRed
                                    else -> TextMuted
                                }
                            )
                        }

                        if (connectedDevice != null && !isDemoMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Dispositivo: ${connectedDevice.name} (${connectedDevice.address})",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        if (connectionState == ObdConnectionState.CONNECTED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onDisconnectDevice,
                                colors = ButtonDefaults.buttonColors(containerColor = GaugeRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_disconnect")
                            ) {
                                Text("Desconectar Adaptador OBD", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Demo Mode Toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CockpitCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CockpitCardBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = GaugeYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Modo Demonstração (Sem OBD)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Simula aceleração dinâmica, RPM, frenagens e modos elétrico/híbrido para testes na bancada.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = onDemoModeToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextWhite,
                                checkedTrackColor = GaugeYellow,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF242A36)
                            ),
                            modifier = Modifier.testTag("toggle_demo_mode")
                        )
                    }
                }
            }

            // 3. Vehicle Profile Selection (recolhível, com filtro por marca)
            item {
                Column {
                    Text(
                        text = "Perfil do Veículo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Ajusta a capacidade do tanque e a referência de consumo Inmetro para cálculo de autonomia.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileRow(
                        p = selectedProfile,
                        isSelected = true,
                        onClick = { profilesExpanded = !profilesExpanded },
                        onEdit = { editingProfile = selectedProfile }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { profilesExpanded = !profilesExpanded },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_toggle_profiles")
                        ) {
                            Icon(
                                imageVector = if (profilesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (profilesExpanded) "Fechar" else "Trocar (${allProfiles.size})")
                        }

                        Button(
                            onClick = { editingProfile = selectedProfile },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GaugeGreen,
                                contentColor = CockpitBlack
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_edit_selected_profile")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Perfil",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editar Perfil", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (profilesExpanded) {
                item {
                    OutlinedTextField(
                        value = profileQuery,
                        onValueChange = { profileQuery = it },
                        label = { Text("Buscar veículo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_search")
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            BrandChip(label = "Todas", selected = selectedBrand == null) { selectedBrand = null }
                        }
                        items(brands) { brand ->
                            BrandChip(label = brand, selected = selectedBrand == brand) {
                                selectedBrand = if (selectedBrand == brand) null else brand
                            }
                        }
                    }
                }

                item {
                    // Lista com altura fixa e rolagem própria: não estica mais a tela inteira
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        if (filteredProfiles.isEmpty()) {
                            Text(
                                text = "Nenhum veículo encontrado.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(filteredProfiles, key = { it.id }) { p ->
                                    ProfileRow(
                                        p = p,
                                        isSelected = p.id == selectedProfile.id,
                                        onClick = {
                                            onProfileSelected(p)
                                            profilesExpanded = false
                                            profileQuery = ""
                                            selectedBrand = null
                                        },
                                        onEdit = {
                                            editingProfile = p
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Bluetooth OBD-II Devices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dispositivos Bluetooth OBD",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Adaptador ELM327 Bluetooth Classic (SPP)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.testTag("btn_refresh_bt")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar dispositivos",
                            tint = GaugeGreen
                        )
                    }
                }
            }

            if (pairedDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CockpitCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum dispositivo OBD pareado encontrado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pareie o seu scanner ELM327 nas configurações Bluetooth do Android e toque no botão atualizar acima.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(pairedDevices) { dev ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bt_device_${dev.address}"),
                        colors = CardDefaults.cardColors(containerColor = CockpitCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CockpitCardBorder))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = dev.address,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            // Só o dispositivo que está conectando (ou conectado) muda de aparência
                            val isConnecting = connectionState == ObdConnectionState.CONNECTING &&
                                dev.address == connectingAddress
                            val isConnected = connectionState == ObdConnectionState.CONNECTED &&
                                dev.address == connectedDevice?.address

                            Button(
                                onClick = {
                                    if (connectionState != ObdConnectionState.CONNECTING) onConnectDevice(dev)
                                },
                                enabled = true,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isConnected -> GaugeGreen
                                        isConnecting -> GaugeYellow
                                        else -> Color(0xFF2563EB)
                                    }
                                ),
                                modifier = Modifier.testTag("btn_connect_${dev.address}")
                            ) {
                                when {
                                    isConnecting -> {
                                        CircularProgressIndicator(
                                            color = TextWhite,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Conectando...")
                                    }
                                    isConnected -> Text("Conectado")
                                    else -> Text("Conectar")
                                }
                            }
                        }
                    }
                }
            }

            // 6. Diagnostics Log Console
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terminal de Diagnóstico OBD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )

                    OutlinedButton(onClick = { showLogs = !showLogs }) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showLogs) "Ocultar" else "Ver Logs", fontSize = 12.sp)
                    }
                }

                if (showLogs) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090C10))
                            .border(1.dp, CockpitCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val text = if (diagnosticLogs.isEmpty()) {
                            "Aguardando comandos OBD..."
                        } else {
                            diagnosticLogs.takeLast(15).joinToString("\n")
                        }
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = GaugeGreen,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (editingProfile != null) {
        ProfileEditDialog(
            profile = editingProfile!!,
            onDismiss = { editingProfile = null },
            onSave = { updated ->
                onProfileUpdated(updated)
                editingProfile = null
            }
        )
    }
}

@Composable
private fun BrandChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) GaugeGreen.copy(alpha = 0.2f) else CockpitCard)
            .border(1.dp, if (selected) GaugeGreen else CockpitCardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) GaugeGreen else TextWhite
        )
    }
}

@Composable
private fun ProfileRow(
    p: VehicleProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("profile_${p.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B2330) else CockpitCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) GaugeGreen else CockpitCardBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = if (isSelected) GaugeGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = p.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${p.engine}  •  Tanque: ${p.tankCapacityLiters.toInt()}L  •  Inmetro: ${p.inmetroUrbanKmPerL} urb / ${p.inmetroRoadKmPerL} rod km/L",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_edit_profile_${p.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar ${p.name}",
                            tint = if (isSelected) GaugeGreen else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GaugeGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selecionado",
                            tint = CockpitBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
