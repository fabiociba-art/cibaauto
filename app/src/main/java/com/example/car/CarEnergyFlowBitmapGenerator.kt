package com.example.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
 * Gerador de diagramas de fluxo de energia vetoriais para o Android Auto.
 * Renderiza um Bitmap em alta definição (480x300 px) com fundo escuro, pista verde diagonal,
 * silhueta do Corolla e condutos de energia iluminados.
 *
 * Cacheado por EnergyMode para garantir taxa de atualização estritamente dentro dos limites do host do carro.
 */
object CarEnergyFlowBitmapGenerator {

    private val bitmapCache = ConcurrentHashMap<EnergyMode, CarIcon>()

    fun getCarIconForMode(mode: EnergyMode, isHybrid: Boolean = true): CarIcon {
        val cached = bitmapCache[mode]
        if (cached != null) return cached

        val bitmap = renderFlowDiagram(mode, isHybrid)
        val icon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
        bitmapCache[mode] = icon
        return icon
    }

    private fun renderFlowDiagram(mode: EnergyMode, isHybrid: Boolean): Bitmap {
        val width = 480
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fundo do Cockpit
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#0C1420"),
                Color.parseColor("#05080E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Faixa Verde Diagonal (Estrada de luz do vídeo da multimídia)
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, height * 0.2f, width.toFloat(), height * 0.9f,
                Color.parseColor("#1500FFCC"),
                Color.parseColor("#2500FF66"),
                Shader.TileMode.CLAMP
            )
        }
        val roadBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6600FFCC")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val roadPath = Path().apply {
            moveTo(-50f, height * 0.25f)
            lineTo(width * 0.75f, 0f)
            lineTo(width + 60f, height * 0.75f)
            lineTo(width * 0.25f, height.toFloat() + 40f)
            close()
        }
        canvas.drawPath(roadPath, roadPaint)
        canvas.drawLine(-50f, height * 0.25f, width * 0.25f, height.toFloat() + 40f, roadBorderPaint)
        canvas.drawLine(width * 0.75f, 0f, width + 60f, height * 0.75f, roadBorderPaint)

        // Listras de velocidade na pista
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3300FFCC")
            strokeWidth = 4f
        }
        for (i in 1..4) {
            val t = i / 5f
            val sx = -50f + (width * 0.25f + 50f) * t
            val sy = height * 0.25f + (height * 0.75f + 40f) * t
            val ex = width * 0.75f + (width * 0.25f + 60f) * t
            val ey = 0f + (height * 0.75f) * t
            canvas.drawLine(sx, sy, ex, ey, stripePaint)
        }

        // 3. Silhueta do Carro (Vista Superior - Sedã Branco)
        val carCx = width * 0.50f
        val carCy = height * 0.50f
        val carW = 120f
        val carH = 250f

        // Sombra sob o carro
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#77000000")
        }
        canvas.drawRoundRect(
            RectF(carCx - carW * 0.55f, carCy - carH * 0.52f, carCx + carW * 0.55f, carCy + carH * 0.52f),
            20f, 20f, shadowPaint
        )

        // Carroceria branca
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
        }
        val carRect = RectF(carCx - carW * 0.50f, carCy - carH * 0.50f, carCx + carW * 0.50f, carCy + carH * 0.50f)
        canvas.drawRoundRect(carRect, 22f, 22f, bodyPaint)

        // Contorno neon do carro de acordo com o modo
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

        // Vidros escurecidos
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawRoundRect(
            RectF(carCx - carW * 0.38f, carCy - carH * 0.35f, carCx + carW * 0.38f, carCy + carH * 0.35f),
            12f, 12f, glassPaint
        )

        // Faróis dianteiros LED
        val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F7FA")
        }
        canvas.drawCircle(carCx - carW * 0.35f, carCy - carH * 0.46f, 6f, headlightPaint)
        canvas.drawCircle(carCx + carW * 0.35f, carCy - carH * 0.46f, 6f, headlightPaint)

        // Lanternas traseiras vermelhas
        val taillightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(carCx - carW * 0.38f, carCy + carH * 0.47f, carCx + carW * 0.38f, carCy + carH * 0.47f, taillightPaint)

        // Rodas pretas
        val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val frontAxleY = carCy - carH * 0.28f
        val rearAxleY = carCy + carH * 0.28f
        val leftWheelX = carCx - carW * 0.52f
        val rightWheelX = carCx + carW * 0.52f

        canvas.drawRoundRect(RectF(leftWheelX - 10f, frontAxleY - 18f, leftWheelX + 10f, frontAxleY + 18f), 4f, 4f, wheelPaint)
        canvas.drawRoundRect(RectF(rightWheelX - 10f, frontAxleY - 18f, rightWheelX + 10f, frontAxleY + 18f), 4f, 4f, wheelPaint)
        canvas.drawRoundRect(RectF(leftWheelX - 10f, rearAxleY - 18f, leftWheelX + 10f, rearAxleY + 18f), 4f, 4f, wheelPaint)
        canvas.drawRoundRect(RectF(rightWheelX - 10f, rearAxleY - 18f, rightWheelX + 10f, rearAxleY + 18f), 4f, 4f, wheelPaint)

        // 4. Componentes e Fluxo de Energia
        val engineY = carCy - carH * 0.32f
        val motorY = carCy - carH * 0.12f
        val batteryY = carCy + carH * 0.22f

        // Condutos
        val flowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        when (mode) {
            EnergyMode.EV -> {
                // Bateria -> Motor -> Rodas (Ciano)
                flowPaint.color = Color.parseColor("#00F0FF")
                canvas.drawLine(carCx, batteryY, carCx, motorY, flowPaint)
                canvas.drawLine(carCx, motorY, carCx, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, leftWheelX, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, rightWheelX, frontAxleY, flowPaint)
            }
            EnergyMode.ENGINE -> {
                // Combustão -> Rodas (Laranja)
                flowPaint.color = Color.parseColor("#FF9100")
                canvas.drawLine(carCx, engineY, carCx, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, leftWheelX, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, rightWheelX, frontAxleY, flowPaint)
            }
            EnergyMode.HYBRID -> {
                // Combustão (Laranja) + Bateria (Ciano) -> Rodas
                flowPaint.color = Color.parseColor("#FF9100")
                canvas.drawLine(carCx, engineY, carCx, frontAxleY, flowPaint)
                flowPaint.color = Color.parseColor("#00F0FF")
                canvas.drawLine(carCx, batteryY, carCx, motorY, flowPaint)
                flowPaint.color = Color.parseColor("#00FFCC")
                canvas.drawLine(carCx, frontAxleY, leftWheelX, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, rightWheelX, frontAxleY, flowPaint)
            }
            EnergyMode.CHARGING -> {
                // Rodas -> Motor -> BATERIA (Verde)
                flowPaint.color = Color.parseColor("#00FF66")
                canvas.drawLine(leftWheelX, frontAxleY, carCx, frontAxleY, flowPaint)
                canvas.drawLine(rightWheelX, frontAxleY, carCx, frontAxleY, flowPaint)
                canvas.drawLine(carCx, frontAxleY, carCx, motorY, flowPaint)
                canvas.drawLine(carCx, motorY, carCx, batteryY, flowPaint)
            }
            EnergyMode.STANDBY -> {
                // Em standby condutos cinzas discretos
                flowPaint.color = Color.parseColor("#334155")
                canvas.drawLine(carCx, engineY, carCx, frontAxleY, flowPaint)
                if (isHybrid) canvas.drawLine(carCx, motorY, carCx, batteryY, flowPaint)
                canvas.drawLine(leftWheelX, frontAxleY, rightWheelX, frontAxleY, flowPaint)
            }
        }

        // Desenhar Nós: Motor a Combustão
        val engineNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mode == EnergyMode.ENGINE || mode == EnergyMode.HYBRID) Color.parseColor("#FF9100") else Color.parseColor("#334155")
        }
        canvas.drawRoundRect(RectF(carCx - 26f, engineY - 14f, carCx + 26f, engineY + 14f), 6f, 6f, engineNodePaint)

        // Desenhar Nós: Motor Elétrico / Gerador
        if (isHybrid) {
            val motorNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (mode) {
                    EnergyMode.CHARGING -> Color.parseColor("#00FF66")
                    EnergyMode.EV, EnergyMode.HYBRID -> Color.parseColor("#00F0FF")
                    else -> Color.parseColor("#334155")
                }
            }
            canvas.drawCircle(carCx, motorY, 15f, motorNodePaint)

            // Desenhar Nós: Bateria HV
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
