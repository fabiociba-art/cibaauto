package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GaugeGreen,
    onPrimary = Color.Black,
    primaryContainer = CockpitCard,
    onPrimaryContainer = TextWhite,
    secondary = GaugeYellow,
    onSecondary = Color.Black,
    tertiary = GaugeRed,
    background = CockpitBlack,
    onBackground = TextWhite,
    surface = CockpitSurface,
    onSurface = TextWhite,
    surfaceVariant = CockpitCard,
    onSurfaceVariant = TextMuted,
    outline = CockpitCardBorder
)

@Composable
fun CibaAutoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
