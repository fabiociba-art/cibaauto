package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CockpitColorScheme =
    darkColorScheme(
        primary = ElectricBlue,
        onPrimary = CockpitBlack,
        primaryContainer = ElectricBlueVariant,
        onPrimaryContainer = TextPrimary,
        secondary = NeonCyan,
        onSecondary = CockpitBlack,
        secondaryContainer = CockpitSurfaceVariant,
        onSecondaryContainer = NeonCyan,
        background = CockpitBlack,
        onBackground = TextPrimary,
        surface = CockpitSurface,
        onSurface = TextPrimary,
        surfaceVariant = CockpitSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        error = StatusRed,
        onError = TextPrimary,
        outline = CockpitCardBorder
    )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = CockpitColorScheme,
      typography = Typography,
      content = content
  )
}
