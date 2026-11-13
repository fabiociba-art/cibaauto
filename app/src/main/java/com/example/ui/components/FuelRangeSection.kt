package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DriveMode
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun FuelRangeSection(
    fuelLevelPercent: Float,
    driveMode: DriveMode,
    autonomyKm: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Upper stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Fuel + Drive Mode
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                // Fuel pump icon + bar
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Combustível",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "►",
                        fontSize = 8.sp,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Fuel horizontal gradient bar
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF222630))
                ) {
                    // Full gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GaugeRed, GaugeYellow, GaugeGreen)
                                )
                            )
                    )

                    // Cover unreached part from right with dark overlay
                    val emptyFraction = (1f - (fuelLevelPercent / 100f)).coerceIn(0f, 1f)
                    if (emptyFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = emptyFraction)
                                .height(8.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color(0xFF14171E))
                        )
                    }

                    // White position marker line
                    val markerFraction = (fuelLevelPercent / 100f).coerceIn(0.05f, 0.98f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = markerFraction)
                            .height(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(8.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color.White)
                        )
                    }
                }

                // Sub-labels for fuel
                Row(
                    modifier = Modifier.width(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Combustível",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Full",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mode badge
                Text(
                    text = driveMode.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = driveMode.color
                )
                Text(
                    text = "Modo de Direção",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            // Right Column: Autonomia
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$autonomyKm km",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Autonomia",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Car on Glowing Energy Road
        val carDrawable = when (driveMode) {
            DriveMode.ECO -> R.drawable.car_flow_green
            DriveMode.NORMAL -> R.drawable.car_flow_yellow
            DriveMode.SPORT -> R.drawable.car_flow_red
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = carDrawable),
                contentDescription = "Veículo na faixa de energia (${driveMode.label})",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
