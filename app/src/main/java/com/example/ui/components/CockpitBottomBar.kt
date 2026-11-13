package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriveMode
import com.example.model.ObdConnectionState
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.viewmodel.AppScreen

@Composable
fun CockpitBottomBar(
    currentScreen: AppScreen,
    connectionState: ObdConnectionState,
    isDemoMode: Boolean,
    driveMode: DriveMode,
    isHybridVehicle: Boolean,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
    hasFaults: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1016))
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Dashboard (Home)
            val isDashboardActive = currentScreen == AppScreen.DASHBOARD
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onScreenSelected(AppScreen.DASHBOARD) }
                    .background(if (isDashboardActive) Color(0xFF1E2430) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("nav_dashboard"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Dashboard",
                        tint = if (isDashboardActive) GaugeGreen else TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isDashboardActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dashboard",
                            fontSize = 11.sp,
                            fontWeight = if (isDashboardActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDashboardActive) TextWhite else TextMuted
                        )
                    }
                }
            }

            // 2. Fluxo Híbrido (Available for Hybrid vehicles)
            if (isHybridVehicle) {
                val isHybridActive = currentScreen == AppScreen.HYBRID_FLOW
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onScreenSelected(AppScreen.HYBRID_FLOW) }
                        .background(if (isHybridActive) Color(0xFF1E2430) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("nav_hybrid_flow"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricCar,
                            contentDescription = "Fluxo Híbrido",
                            tint = if (isHybridActive) GaugeCyan else GaugeCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        if (isHybridActive) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fluxo Híbrido",
                                fontSize = 11.sp,
                                fontWeight = if (isHybridActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHybridActive) TextWhite else TextMuted
                            )
                        }
                    }
                }
            }

            // 3. Drive mode quick switch pill (supplied by ECU)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161A22))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("quick_mode_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(driveMode.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = driveMode.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = driveMode.color
                    )
                }
            }

            // 3b. Diagnóstico (somente ícone, com ponto vermelho quando há falhas)
            val isDiagActive = currentScreen == AppScreen.DIAGNOSTICS
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onScreenSelected(AppScreen.DIAGNOSTICS) }
                    .background(if (isDiagActive) Color(0xFF1E2430) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("nav_diagnostics"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Diagnóstico",
                    tint = if (hasFaults) GaugeRed else if (isDiagActive) GaugeGreen else TextWhite,
                    modifier = Modifier.size(22.dp)
                )
                if (hasFaults) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GaugeRed)
                    )
                }
            }

            // 4. Configurações (Settings)
            val isSettingsActive = currentScreen == AppScreen.SETTINGS
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onScreenSelected(AppScreen.SETTINGS) }
                    .background(if (isSettingsActive) Color(0xFF1E2430) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("nav_settings"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = if (isSettingsActive) GaugeGreen else TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isSettingsActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Configurações",
                            fontSize = 11.sp,
                            fontWeight = if (isSettingsActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSettingsActive) TextWhite else TextMuted
                        )
                    }
                }
            }
        }
    }
}
