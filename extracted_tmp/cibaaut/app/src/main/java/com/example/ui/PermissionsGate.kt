package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Solicita todas as permissões necessárias para OBD (Bluetooth + Localização)
 * e opcionalmente notificações (Android 13+).
 * Só libera o conteúdo do app depois que o usuário responder (aceitar ou pular).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(content: @Composable () -> Unit) {
    val permissionList = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissionList)
    var userDismissed by remember { mutableStateOf(false) }

    // Pede automaticamente na primeira composição se ainda não tiver tudo
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted && !userDismissed) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    when {
        permissionsState.allPermissionsGranted || userDismissed -> {
            content()
        }
        else -> {
            val perms = permissionsState.permissions
            val bluetoothOk = perms
                .filter {
                    it.permission == Manifest.permission.BLUETOOTH_CONNECT ||
                        it.permission == Manifest.permission.BLUETOOTH_SCAN
                }
                .let { list -> list.isEmpty() || list.all { it.status.isGranted } }
            val locationOk = perms
                .filter {
                    it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                        it.permission == Manifest.permission.ACCESS_COARSE_LOCATION
                }
                .any { it.status.isGranted }
            val notificationOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.any {
                    it.permission == Manifest.permission.POST_NOTIFICATIONS && it.status.isGranted
                }
            } else true

            PermissionRationaleScreen(
                onRequest = { permissionsState.launchMultiplePermissionRequest() },
                onSkip = { userDismissed = true },
                bluetoothOk = bluetoothOk,
                locationOk = locationOk,
                notificationOk = notificationOk
            )
        }
    }
}

@Composable
private fun PermissionRationaleScreen(
    onRequest: () -> Unit,
    onSkip: () -> Unit,
    bluetoothOk: Boolean,
    locationOk: Boolean,
    notificationOk: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "PERMISSÕES NECESSÁRIAS",
            color = ElectricBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "CIBA AUTO precisa de acesso para conectar no ELM327 e registrar viagens.",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        PermissionRow(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            subtitle = "Conectar ao adaptador OBD-2 ELM327",
            granted = bluetoothOk
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = Icons.Default.LocationOn,
            title = "Localização",
            subtitle = "GPS para viagens e busca de dispositivos Bluetooth",
            granted = locationOk
        )
        Spacer(modifier = Modifier.height(10.dp))
        PermissionRow(
            icon = Icons.Default.Notifications,
            title = "Notificações",
            subtitle = "Alertas de temperatura, tensão e falhas",
            granted = notificationOk
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            Text(
                text = "CONCEDER PERMISSÕES",
                color = CockpitBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text(
                text = "Continuar sem permissões (modo limitado)",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CockpitSurface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (granted) StatusGreen else ElectricBlue, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            text = if (granted) "OK" else "Pendente",
            color = if (granted) StatusGreen else StatusAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
