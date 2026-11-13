package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TelemetryData
import kotlin.math.roundToInt

private const val TANK_CAPACITY_LITERS = 43.0f
private const val RESERVE_LITERS = 9.0f
private const val TOTAL_SQUARES = 8
private const val LITERS_PER_SQUARE = TANK_CAPACITY_LITERS / TOTAL_SQUARES // ~5.375 L

// Cores do mostrador digital
private val ColorActiveNormal = Color(0xFF00E5FF) // Ciano elétrico digital
private val ColorActiveReserve = Color(0xFFFF3D00) // Laranja/Vermelho alerta para reserva
private val ColorInactiveSquare = Color(0xFF131822) // Quadrado apagado
private val ColorInactiveBorder = Color(0xFF222B3A)

/**
 * Mostrador de combustível digital no padrão do painel automotivo:
 * - 8 quadrados digitais que vão se apagando da direita para a esquerda conforme o consumo.
 * - Tanque de 43 Litros.
 * - 9 Litros de reserva: quando o combustível atinge a reserva (<= 9L), os blocos restantes
 *   mudam para cor de alerta e um aviso proeminente "CARRO NA RESERVA" é exibido.
 */
@Composable
fun DigitalFuelGauge(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier,
    tankCapacity: Float = TANK_CAPACITY_LITERS,
    reserveThreshold: Float = RESERVE_LITERS
) {
    val effectiveLiters = if (!telemetry.fuelLiters.isNaN() && telemetry.fuelLiters > 0f) {
        telemetry.fuelLiters.coerceIn(0f, tankCapacity)
    } else {
        (tankCapacity * (telemetry.fuelLevelPercent / 100f)).coerceIn(0f, tankCapacity)
    }

    val isAvailable = telemetry.fuelLevelAvailable
    val isReserve = isAvailable && (effectiveLiters <= reserveThreshold || telemetry.fuelLevelPercent <= (reserveThreshold / tankCapacity * 100f))

    // Quantidade de quadrados acesos (0 a 8)
    val activeCount = when {
        !isAvailable -> 0
        effectiveLiters <= 0.2f -> 0
        effectiveLiters <= LITERS_PER_SQUARE -> 1
        else -> ((effectiveLiters / tankCapacity) * TOTAL_SQUARES).roundToInt().coerceIn(1, TOTAL_SQUARES)
    }

    // Animação de pulso suave para o aviso de reserva
    val infiniteTransition = rememberInfiniteTransition(label = "reserve_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier.testTag("digital_fuel_gauge"),
        horizontalAlignment = Alignment.Start
    ) {
        // Linha superior: Ícone da bomba + seta para bocal + leitura em Litros / %
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = "Combustível",
                tint = if (isReserve) ColorActiveReserve.copy(alpha = pulseAlpha) else Color.White,
                modifier = Modifier.size(15.dp)
            )
            // Seta indicando o lado do tanque no Corolla (lado esquerdo ◄)
            Text(
                text = "◄",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReserve) ColorActiveReserve else Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            val litersText = if (isAvailable) "%.1f L · %d%%".format(effectiveLiters, telemetry.fuelLevelPercent.toInt()) else "-- L · --%"
            Text(
                text = litersText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReserve) ColorActiveReserve else Color.White
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grade dos 8 Quadrados Digitais que vão se apagando
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until TOTAL_SQUARES) {
                val isSquareLit = i < activeCount
                // Os primeiros 2 quadrados (0 e 1) cobrem até 10.75 L (zona de reserva de 9 L)
                val isReserveZone = i < 2

                val squareColor = when {
                    !isSquareLit -> ColorInactiveSquare
                    isReserve -> ColorActiveReserve.copy(alpha = if (isReserveZone) pulseAlpha else 1f)
                    isReserveZone -> Color(0xFFFFA000) // Laranja reserva quando cheio ou normal
                    else -> ColorActiveNormal // Ciano elétrico
                }

                val borderColor = when {
                    !isSquareLit -> ColorInactiveBorder
                    isReserve -> ColorActiveReserve
                    isReserveZone -> Color(0xFFFFB300)
                    else -> Color(0xFF80D8FF)
                }

                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 11.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(squareColor)
                        .border(1.dp, borderColor, RoundedCornerShape(2.dp))
                ) {
                    if (isSquareLit) {
                        // Brilho interior digital
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color.White.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Legenda de escala (E ... R (9L) ... F (43L))
        Row(
            modifier = Modifier.width(136.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("E", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isReserve) ColorActiveReserve else Color(0xFF78909C))
            Text("R 9L", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = ColorActiveReserve)
            Text("F 43L", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78909C))
        }

        // AVISO PROEMINENTE QUANDO O CARRO ESTÁ NA RESERVA (<= 9 LITROS)
        if (isReserve) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorActiveReserve.copy(alpha = 0.16f))
                    .border(1.dp, ColorActiveReserve.copy(alpha = pulseAlpha), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ColorActiveReserve,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "CARRO NA RESERVA (%.1f L)".format(effectiveLiters),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorActiveReserve
                )
            }
        }
    }
}
