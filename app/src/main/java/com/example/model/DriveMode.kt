package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ModeEco
import com.example.ui.theme.ModeNormal
import com.example.ui.theme.ModeSport

enum class DriveMode(val label: String, val color: Color, val energyState: String) {
    ECO("ECO", ModeEco, "Elétrico / Regeneração"),
    NORMAL("NORMAL", ModeNormal, "Híbrido Inteligente"),
    SPORT("SPORT", ModeSport, "Combustão / Alta Potência")
}

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val isBonded: Boolean = true,
    val isConnected: Boolean = false
)

enum class ObdConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
