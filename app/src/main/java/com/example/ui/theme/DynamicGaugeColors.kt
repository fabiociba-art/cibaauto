package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Cores dinâmicas — limites preenchidos pelo usuário.
 */
object DynamicGaugeColors {

    private val green = Color(0xFF00CC55)
    private val yellow = Color(0xFFFFAA00)
    private val red = Color(0xFFFF3333)

    fun forSpeed(speed: Float, max: Float = 200f): Color {
        // Verde 0-50, Amarelo 51-90, Vermelho 91-220
        return when {
            speed <= 50f -> green
            speed <= 90f -> lerp(green, yellow, (speed - 50f) / 40f)
            else -> lerp(yellow, red, ((speed - 90f) / 80f).coerceIn(0f, 1f))
        }
    }

    fun forRpm(rpm: Float, max: Float = 8000f): Color {
        // Verde 0-1500, Amarelo 1501-3000, Vermelho 3001+
        return when {
            rpm <= 1500f -> green
            rpm <= 3000f -> lerp(green, yellow, (rpm - 1500f) / 1500f)
            else -> lerp(yellow, red, ((rpm - 3000f) / 2000f).coerceIn(0f, 1f))
        }
    }

    fun forBatteryVoltage(volts: Float): Color {
        // Verde 12-15, Amarelo 11-11.9, Vermelho <=10.9
        return when {
            volts >= 12.0f && volts <= 15.0f -> green
            volts >= 11.0f && volts < 12.0f -> lerp(yellow, green, (volts - 11.0f) / 1.0f)
            else -> red
        }
    }

    fun forCoolant(temp: Float): Color {
        // Verde 0-80, Amarelo 81-100, Vermelho 101+
        return when {
            temp <= 80f -> green
            temp <= 100f -> lerp(green, yellow, (temp - 80f) / 20f)
            else -> lerp(yellow, red, ((temp - 100f) / 50f).coerceIn(0f, 1f))
        }
    }

    fun forLoad(percent: Float): Color {
        // Verde 0-30, Amarelo 31-70, Vermelho 71-100
        return when {
            percent <= 30f -> green
            percent <= 70f -> lerp(green, yellow, (percent - 30f) / 40f)
            else -> lerp(yellow, red, ((percent - 70f) / 30f).coerceIn(0f, 1f))
        }
    }

    fun forThrottle(percent: Float): Color {
        // Verde 0-25, Amarelo 26-60, Vermelho 61-100
        return when {
            percent <= 25f -> green
            percent <= 60f -> lerp(green, yellow, (percent - 25f) / 35f)
            else -> lerp(yellow, red, ((percent - 60f) / 40f).coerceIn(0f, 1f))
        }
    }

    fun forLoadOrThrottle(percent: Float): Color = forLoad(percent)

    fun forConsumption(kml: Float, reference: Float = 15.7f): Color {
        // Verde até a referência Inmetro, abaixo vai para amarelo e vermelho
        val yellowStart = reference * 0.75f
        val redStart = reference * 0.50f
        return when {
            kml >= reference -> green
            kml >= yellowStart -> lerp(yellow, green, (kml - yellowStart) / (reference - yellowStart))
            kml >= redStart -> lerp(red, yellow, (kml - redStart) / (yellowStart - redStart))
            else -> red
        }
    }

    fun forSoc(percent: Float): Color {
        // Verde 80-100, Amarelo 50-79, Vermelho 0-49
        return when {
            percent >= 80f -> green
            percent >= 50f -> lerp(yellow, green, (percent - 50f) / 30f)
            else -> lerp(red, yellow, (percent / 50f).coerceIn(0f, 1f))
        }
    }

    fun forGeneric(value: Float, max: Float, higherIsBetter: Boolean = false): Color {
        val p = (value / max).coerceIn(0f, 1f)
        val t = if (higherIsBetter) 1f - p else p
        return when {
            t < 0.3f -> green
            t < 0.6f -> lerp(green, yellow, (t - 0.3f) / 0.3f)
            else -> lerp(yellow, red, (t - 0.6f) / 0.4f)
        }
    }
}
