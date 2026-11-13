package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import com.example.ui.components.CorollaEnergyFlowDiagram
import com.example.ui.components.MiniGauge
import com.example.ui.components.MiniGaugeCenterType
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitCardBorder
import com.example.ui.theme.GaugeCyan
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun HybridEnergyFlowScreen(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .statusBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_hybrid_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar ao Dashboard",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Fluxo de Energia Corolla Hybrid",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurações",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Top 3 Key Stats Row (Velocidade média, Tempo decorrido, Autonomia)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${telemetry.avgSpeedKmh.toInt()} km/h",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Velocidade média",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = telemetry.formattedElapsedTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Tempo decorrido",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = telemetry.autonomyText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Autonomia",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Diagrama do Trem de Força Híbrido idêntico ao painel do Toyota Corolla
        // Removidos os 4 quadrados de cima conforme solicitado pelo usuário
        CorollaEnergyFlowDiagram(
            telemetry = telemetry,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Nível da Bateria Híbrida (SOC) mantido logo abaixo do diagrama
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("card_battery_soc"),
            colors = CardDefaults.cardColors(containerColor = CockpitCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CockpitCardBorder))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = GaugeCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nível da Bateria Híbrida (SOC)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Text(
                        text = if (telemetry.hybridSocAvailable) "${telemetry.hybridBatterySocPercent.toInt()}%" else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = GaugeCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { if (telemetry.hybridSocAvailable) (telemetry.hybridBatterySocPercent / 100f).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = GaugeCyan,
                    trackColor = Color(0xFF1E2430)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (telemetry.hybridDetailAvailable) "Tensão: %.1fV".format(telemetry.hybridBatteryVoltageVolts) else "Tensão: --",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = if (telemetry.hybridBatteryTempAvailable && !telemetry.hybridBatteryTempC.isNaN()) "Temp: %.1f°C".format(telemetry.hybridBatteryTempC) else "Temp: --",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = if (!telemetry.hybridDetailAvailable) "--" else if (telemetry.hybridBatteryCurrentAmps < 0) "Recarregando" else "Descarregando",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (telemetry.hybridBatteryCurrentAmps < 0) GaugeGreen else GaugeYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Sensores e Telemetria em Tempo Real (Etanol, Água, Bateria 12V)
        Text(
            text = "Sensores e Telemetria em Tempo Real",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // % Etanol na gasolina
            MiniGauge(
                value = telemetry.ethanolPercent,
                minValue = 0f,
                maxValue = 100f,
                label = "% Etanol na gasolina",
                unit = "%",
                centerType = MiniGaugeCenterType.ICON_ETHANOL,
                displayValue = if (telemetry.ethanolPercent.isNaN()) "--" else "${telemetry.ethanolPercent.toInt()}%",
                size = 100.dp,
                modifier = Modifier.testTag("gauge_ethanol_hybrid")
            )

            // Temperatura d'Água
            MiniGauge(
                value = telemetry.coolantTempC,
                minValue = 0f,
                maxValue = 300f,
                label = "Água",
                unit = "°C",
                centerType = MiniGaugeCenterType.ICON_COOLANT,
                displayValue = "${telemetry.coolantTempC.toInt()}°C",
                size = 100.dp,
                modifier = Modifier.testTag("gauge_coolant_hybrid")
            )

            // Tensão 12V
            MiniGauge(
                value = telemetry.batteryVoltage,
                minValue = 0f,
                maxValue = 16f,
                label = "Bateria 12V",
                unit = "V",
                centerType = MiniGaugeCenterType.NEEDLE_BATTERY,
                displayValue = "%.1fV".format(telemetry.batteryVoltage),
                size = 100.dp,
                modifier = Modifier.testTag("gauge_battery_hybrid")
            )
        }

        // Temperatura da bateria de alta tensão: só aparece com leitura real (ou no demo)
        if (telemetry.hybridBatteryTempAvailable && !telemetry.hybridBatteryTempC.isNaN()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniGauge(
                    value = telemetry.hybridBatteryTempC,
                    minValue = 0f,
                    maxValue = 80f,
                    label = "Bateria HV",
                    unit = "°C",
                    centerType = MiniGaugeCenterType.ICON_COOLANT,
                    displayValue = "${telemetry.hybridBatteryTempC.toInt()}°C",
                    size = 100.dp,
                    modifier = Modifier.testTag("gauge_hv_temp")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
