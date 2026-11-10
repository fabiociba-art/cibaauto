package com.example.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import java.util.concurrent.ConcurrentHashMap

/**
 * Medidores Android Auto — tamanho 2x e cores iguais ao app.
 */
object CarGaugeBitmapGenerator {

    private val cache = ConcurrentHashMap<String, CarIcon>()

    private val green = Color.parseColor("#00CC55")
    private val yellow = Color.parseColor("#FFAA00")
    private val red = Color.parseColor("#FF3333")

    fun gaugeIcon(
        value: Float,
        max: Float,
        label: String,
        unit: String,
        colorMode: String = "normal"
    ): CarIcon {
        val key = "${label}_${value.toInt()}_${colorMode}"
        cache[key]?.let { return it }

        // 2x o tamanho anterior (era 240)
        val size = 480
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val progress = (value / max).coerceIn(0f, 1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 44f
            strokeCap = Paint.Cap.ROUND
        }
        val rect = RectF(56f, 56f, size - 56f, size - 56f)

        // Track
        paint.color = Color.parseColor("#1A2332")
        canvas.drawArc(rect, 135f, 270f, false, paint)

        // Progress com cor por valor
        if (progress > 0f) {
            val segments = 32
            val totalSweep = 270f * progress
            val segSweep = totalSweep / segments
            for (i in 0 until segments) {
                val absoluteValue = ((i + 0.5f) / segments * progress) * max
                paint.color = colorForValue(absoluteValue, max, colorMode, label)
                canvas.drawArc(rect, 135f + i * segSweep, segSweep + 0.8f, false, paint)
            }
        }

        // Valor grande
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 84f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(formatValue(value), size / 2f, size / 2f + 28f, textPaint)

        // Unidade
        textPaint.textSize = 32f
        textPaint.color = Color.parseColor("#94A3B8")
        canvas.drawText(unit, size / 2f, size / 2f + 72f, textPaint)

        val icon = CarIcon.Builder(IconCompat.createWithBitmap(bmp)).build()
        cache[key] = icon
        if (cache.size > 30) cache.clear()
        return icon
    }

    private fun formatValue(v: Float): String =
        if (v >= 100f) v.toInt().toString() else String.format("%.1f", v)

    private fun colorForValue(value: Float, max: Float, mode: String, label: String): Int {
        val isSpeed = label.contains("VEL", true) || mode == "speed" || (max in 150f..250f)
        val isRpm = label.contains("RPM", true) || max >= 500f
        val isBattery = mode == "battery" || label.contains("BAT", true)
        val isTemp = mode == "temp" || label.contains("AGUA", true) || label.contains("ÁGUA", true)
        val isLoad = mode == "load" || label.contains("CARGA", true)
        val isThrottle = mode == "throttle" || label.contains("ACEL", true)
        val isConsumption = mode == "consumption" || label.contains("KML", true) || label.contains("CONSUMO", true)
        val isSoc = mode == "soc" || label.contains("SOC", true)

        return when {
            isSpeed -> when {
                value <= 50f -> green
                value <= 90f -> lerp(green, yellow, (value - 50f) / 40f)
                else -> lerp(yellow, red, ((value - 90f) / 80f).coerceIn(0f, 1f))
            }
            isRpm -> when {
                value <= 1500f -> green
                value <= 3000f -> lerp(green, yellow, (value - 1500f) / 1500f)
                else -> lerp(yellow, red, ((value - 3000f) / 2000f).coerceIn(0f, 1f))
            }
            isBattery -> when {
                value >= 12.0f && value <= 15.0f -> green
                value >= 11.0f && value < 12.0f -> lerp(yellow, green, (value - 11.0f) / 1.0f)
                else -> red
            }
            isTemp -> when {
                value <= 80f -> green
                value <= 100f -> lerp(green, yellow, (value - 80f) / 20f)
                else -> lerp(yellow, red, ((value - 100f) / 50f).coerceIn(0f, 1f))
            }
            isLoad -> when {
                value <= 30f -> green
                value <= 70f -> lerp(green, yellow, (value - 30f) / 40f)
                else -> lerp(yellow, red, ((value - 70f) / 30f).coerceIn(0f, 1f))
            }
            isThrottle -> when {
                value <= 25f -> green
                value <= 60f -> lerp(green, yellow, (value - 25f) / 35f)
                else -> lerp(yellow, red, ((value - 60f) / 40f).coerceIn(0f, 1f))
            }
            isConsumption -> {
                // Referência Corolla Hybrid gasolina (18.5)
                val ref = 18.5f
                val yellowStart = ref * 0.75f
                val redStart = ref * 0.50f
                when {
                    value >= ref -> green
                    value >= yellowStart -> lerp(yellow, green, (value - yellowStart) / (ref - yellowStart))
                    value >= redStart -> lerp(red, yellow, (value - redStart) / (yellowStart - redStart))
                    else -> red
                }
            }
            isSoc -> when {
                value >= 80f -> green
                value >= 50f -> lerp(yellow, green, (value - 50f) / 30f)
                else -> lerp(red, yellow, (value / 50f).coerceIn(0f, 1f))
            }
            else -> when {
                value <= 30f -> green
                value <= 60f -> lerp(green, yellow, (value - 30f) / 30f)
                else -> lerp(yellow, red, ((value - 60f) / 40f).coerceIn(0f, 1f))
            }
        }
    }

    private fun lerp(c1: Int, c2: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        val a = (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * tt).toInt()
        val r = (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * tt).toInt()
        val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * tt).toInt()
        val b = (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * tt).toInt()
        return Color.argb(a, r, g, b)
    }
}
