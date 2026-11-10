package com.example.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.example.model.EnergyMode
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerador de diagramas de fluxo de energia para Android Auto.
 * Suporta fase de animação (phase 0..1) para simular movimento das linhas
 * com DashPathEffect. Cores alinhadas com o app do celular.
 */
object CarEnergyFlowBitmapGenerator {

    private val cache = ConcurrentHashMap<String, CarIcon>()

    fun getCarIconForMode(mode: EnergyMode, isHybrid: Boolean = true, phase: Float = 0f): CarIcon {
        val phaseKey = (phase * 8).toInt() % 8
        val key = "${mode.name}_${isHybrid}_$phaseKey"
        cache[key]?.let { return it }

        val bitmap = renderFlowDiagram(mode, isHybrid, phase)
        val icon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
        cache[key] = icon
        if (cache.size > 40) cache.clear()
        return icon
    }

    private fun renderFlowDiagram(mode: EnergyMode, isHybrid: Boolean, phase: Float): Bitmap {
        val width = 480
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fundo
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#0C1420"),
                Color.parseColor("#05080E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val carCx = width * 0.50f
        val carCy = height * 0.50f
        val carW = 120f
        val carH = 250f

        // Carroceria
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F5F9") }
        val carRect = RectF(carCx - carW * 0.50f, carCy - carH * 0.50f, carCx + carW * 0.50f, carCy + carH * 0.50f)
        canvas.drawRoundRect(carRect, 22f, 22f, bodyPaint)

        val neonColor = when (mode) {
            EnergyMode.EV -> Color.parseColor("#00F0FF")
            EnergyMode.CHARGING -> Color.parseColor("#00FF66")
            EnergyMode.HYBRID -> Color.parseColor("#00FFCC")
            EnergyMode.ENGINE -> Color.parseColor("#FF9100")
            EnergyMode.STANDBY -> Color.parseColor("#64748B")
        }
        val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = neonColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(carRect, 22f, 22f, neonPaint)

        // Vidros
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
        canvas.drawRoundRect(
            RectF(carCx - carW * 0.38f, carCy - carH * 0.35f, carCx + carW * 0.38f, carCy + carH * 0.35f),
            12f, 12f, glassPaint
        )

        // Faróis
        val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0F7FA") }
        canvas.drawCircle(carCx - carW * 0.35f, carCy - carH * 0.46f, 6f, headlightPaint)
        canvas.drawCircle(carCx + carW * 0.35f, carCy - carH * 0.46f, 6f, headlightPaint)

        // Lanternas
        val taillightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(carCx - carW * 0.38f, carCy + carH * 0.47f, carCx + carW * 0.38f, carCy + carH * 0.47f, taillightPaint)

        // Rodas
        val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B") }
        val frontAxleY = carCy - carH * 0.28f
        val rearAxleY = carCy + carH * 0.28f
        val leftWheelX = carCx - carW * 0.52f
        val rightWheelX = carCx + carW * 0.52f

        fun drawWheel(x: Float, y: Float) {
            canvas.drawRoundRect(RectF(x - 10f, y - 18f, x + 10f, y + 18f), 4f, 4f, wheelPaint)
        }
        drawWheel(leftWheelX, frontAxleY)
        drawWheel(rightWheelX, frontAxleY)
        drawWheel(leftWheelX, rearAxleY)
        drawWheel(rightWheelX, rearAxleY)

        // Componentes
        val engineY = carCy - carH * 0.32f
        val motorY = carCy - carH * 0.12f
        val batteryY = carCy + carH * 0.22f

        // Paint animado com dash
        val dashLen = 18f
        val gapLen = 10f
        val phaseOffset = phase * (dashLen + gapLen)

        fun makeFlowPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(dashLen, gapLen), phaseOffset)
        }

        fun drawFlowLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
            canvas.drawLine(x1, y1, x2, y2, makeFlowPaint(color))
        }

        when (mode) {
            EnergyMode.EV -> {
                val c = Color.parseColor("#00F0FF")
                drawFlowLine(carCx, batteryY, carCx, motorY, c)
                drawFlowLine(carCx, motorY, carCx, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, leftWheelX, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, rightWheelX, frontAxleY, c)
            }
            EnergyMode.ENGINE -> {
                val c = Color.parseColor("#FF9100")
                drawFlowLine(carCx, engineY, carCx, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, leftWheelX, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, rightWheelX, frontAxleY, c)
            }
            EnergyMode.HYBRID -> {
                drawFlowLine(carCx, engineY, carCx, frontAxleY, Color.parseColor("#FF9100"))
                drawFlowLine(carCx, batteryY, carCx, motorY, Color.parseColor("#00F0FF"))
                val c = Color.parseColor("#00FFCC")
                drawFlowLine(carCx, frontAxleY, leftWheelX, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, rightWheelX, frontAxleY, c)
            }
            EnergyMode.CHARGING -> {
                val c = Color.parseColor("#00FF66")
                drawFlowLine(leftWheelX, frontAxleY, carCx, frontAxleY, c)
                drawFlowLine(rightWheelX, frontAxleY, carCx, frontAxleY, c)
                drawFlowLine(carCx, frontAxleY, carCx, motorY, c)
                drawFlowLine(carCx, motorY, carCx, batteryY, c)
            }
            EnergyMode.STANDBY -> {
                val c = Color.parseColor("#334155")
                drawFlowLine(carCx, engineY, carCx, frontAxleY, c)
                if (isHybrid) drawFlowLine(carCx, motorY, carCx, batteryY, c)
                drawFlowLine(leftWheelX, frontAxleY, rightWheelX, frontAxleY, c)
            }
        }

        // Nós
        val engineNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mode == EnergyMode.ENGINE || mode == EnergyMode.HYBRID) Color.parseColor("#FF9100") else Color.parseColor("#334155")
        }
        canvas.drawRoundRect(RectF(carCx - 26f, engineY - 14f, carCx + 26f, engineY + 14f), 6f, 6f, engineNodePaint)

        if (isHybrid) {
            val motorNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (mode) {
                    EnergyMode.CHARGING -> Color.parseColor("#00FF66")
                    EnergyMode.EV, EnergyMode.HYBRID -> Color.parseColor("#00F0FF")
                    else -> Color.parseColor("#334155")
                }
            }
            canvas.drawCircle(carCx, motorY, 15f, motorNodePaint)

            val batteryNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (mode) {
                    EnergyMode.CHARGING -> Color.parseColor("#00FF66")
                    EnergyMode.EV, EnergyMode.HYBRID -> Color.parseColor("#00E5FF")
                    else -> Color.parseColor("#334155")
                }
            }
            canvas.drawRoundRect(RectF(carCx - 32f, batteryY - 14f, carCx + 32f, batteryY + 14f), 6f, 6f, batteryNodePaint)
        }

        return bitmap
    }
}
