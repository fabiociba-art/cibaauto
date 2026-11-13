package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Warning
import com.example.data.InmetroConsumption
import com.example.model.DTCLevel
import com.example.model.DiagnosticsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.model.GaugeCatalog
import com.example.ui.components.ConfigurableGauge
import com.example.ui.components.GaugePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TelemetryData
import com.example.model.VehicleProfile
import com.example.ui.components.AnimatedEnergyRoad
import com.example.ui.components.DigitalFuelGauge
import com.example.ui.components.MiniGauge
import com.example.ui.components.MiniGaugeCenterType
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TachometerGauge
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeRed
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun DashboardScreen(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    diagnostics: DiagnosticsState = DiagnosticsState(),
    onOpenDiagnostics: () -> Unit = {},
    gaugeSlots: List<String> = GaugeCatalog.DEFAULT_SLOTS,
    onGaugeSlotChanged: (Int, String) -> Unit = { _, _ -> },
    onResetGauges: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Posição do manômetro que o usuário tocou (null = lista fechada)
    var editingSlot by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
    ) {
        if (isLandscape) {
            LandscapeDashboardLayout(
                telemetry = telemetry,
                profile = profile,
                onOpenSettings = onOpenSettings,
                diagnostics = diagnostics,
                onOpenDiagnostics = onOpenDiagnostics,
                gaugeSlots = gaugeSlots,
                onGaugeClick = { editingSlot = it }
            )
        } else {
            PortraitDashboardLayout(
                telemetry = telemetry,
                profile = profile,
                onOpenSettings = onOpenSettings,
                diagnostics = diagnostics,
                onOpenDiagnostics = onOpenDiagnostics,
                gaugeSlots = gaugeSlots,
                onGaugeClick = { editingSlot = it }
            )
        }

        editingSlot?.let { slot ->
            GaugePickerDialog(
                currentId = gaugeSlots.getOrNull(slot) ?: GaugeCatalog.DEFAULT_SLOTS[slot],
                telemetry = telemetry,
                onSelect = { id ->
                    onGaugeSlotChanged(slot, id)
                    editingSlot = null
                },
                onReset = {
                    onResetGauges()
                    editingSlot = null
                },
                onDismiss = { editingSlot = null }
            )
        }
    }
}

/** Manômetro de uma posição fixa do dashboard; o sensor vem da configuração salva. */
@Composable
private fun SlotGauge(
    slot: Int,
    gaugeSlots: List<String>,
    telemetry: TelemetryData,
    size: androidx.compose.ui.unit.Dp,
    onGaugeClick: (Int) -> Unit
) {
    val sensor = gaugeSlots.getOrNull(slot)?.let { GaugeCatalog.byId[it] }
        ?: GaugeCatalog.byId.getValue(GaugeCatalog.DEFAULT_SLOTS[slot])
    ConfigurableGauge(
        sensor = sensor,
        telemetry = telemetry,
        size = size,
        onClick = { onGaugeClick(slot) },
        modifier = Modifier.testTag("gauge_slot_$slot")
    )
}

@Composable
private fun PortraitDashboardLayout(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    onOpenSettings: () -> Unit,
    diagnostics: DiagnosticsState,
    onOpenDiagnostics: () -> Unit,
    gaugeSlots: List<String>,
    onGaugeClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("btn_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.driving_data),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("btn_quick_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurações",
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        FaultBanner(
            diagnostics = diagnostics,
            onClick = onOpenDiagnostics,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // 1. Manômetros Superiores Lado a Lado (Conforme solicitado pelo usuário)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Velocímetro (Esquerda)
            SpeedometerGauge(
                currentSpeed = telemetry.speedKmh,
                avgSpeed = telemetry.avgSpeedKmh,
                maxSpeed = profile.maxSpeed,
                size = 152.dp,
                modifier = Modifier.testTag("gauge_speedometer")
            )

            // Tacômetro (Direita)
            TachometerGauge(
                currentRpm = telemetry.rpm,
                elapsedTimeFormatted = telemetry.formattedElapsedTime,
                maxRpm = profile.maxRpm,
                size = 152.dp,
                modifier = Modifier.testTag("gauge_tachometer")
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 1b. Consumo médio (sob o velocímetro) e instantâneo (sob o tacômetro), lado a lado
        ConsumptionRow(
            telemetry = telemetry,
            profile = profile,
            modifier = Modifier.testTag("consumption_row")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Linha Intermediária: Combustível + Autonomia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mostrador de Combustível Digital: 8 quadrados que se apagam, 43L total, 9L reserva
            DigitalFuelGauge(
                telemetry = telemetry,
                tankCapacity = profile.tankCapacityLiters.toFloat().takeIf { it > 10f } ?: 43.0f,
                reserveThreshold = 9.0f,
                modifier = Modifier.testTag("digital_fuel_gauge_portrait")
            )

            // Autonomia
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = telemetry.autonomyText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = stringResource(R.string.range),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Faixa de Energia Colorida Contínua com Setas Animadas (cor orientada pelo pedal do acelerador)
        val pedalPortrait = telemetry.sensors["pedal_d"] ?: telemetry.sensors["pedal_e"]
        val (accelPortrait, isPedalPortrait) = if (pedalPortrait != null && !pedalPortrait.isNaN()) {
            Pair(pedalPortrait, true)
        } else {
            Pair(telemetry.throttlePercent, false)
        }
        AnimatedEnergyRoad(
            driveMode = telemetry.driveMode,
            speedKmh = telemetry.speedKmh,
            accelPercent = accelPortrait,
            isPedal = isPedalPortrait,
            ecuLinkOk = telemetry.ecuLinkOk,
            modifier = Modifier.testTag("animated_energy_road")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Mostradores abaixo do carro (toque em um deles para trocar o sensor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlotGauge(0, gaugeSlots, telemetry, 100.dp, onGaugeClick)
            SlotGauge(1, gaugeSlots, telemetry, 100.dp, onGaugeClick)
            SlotGauge(2, gaugeSlots, telemetry, 100.dp, onGaugeClick)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlotGauge(3, gaugeSlots, telemetry, 100.dp, onGaugeClick)
            if (profile.isHybrid) {
                SlotGauge(4, gaugeSlots, telemetry, 100.dp, onGaugeClick)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun LandscapeDashboardLayout(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    onOpenSettings: () -> Unit,
    diagnostics: DiagnosticsState,
    onOpenDiagnostics: () -> Unit,
    gaugeSlots: List<String>,
    onGaugeClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coluna Esquerda: Velocímetro e Tacômetro Lado a Lado ou verticais compactos
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            SpeedometerGauge(
                currentSpeed = telemetry.speedKmh,
                avgSpeed = telemetry.avgSpeedKmh,
                maxSpeed = profile.maxSpeed,
                size = 145.dp,
                modifier = Modifier.testTag("gauge_speedometer_landscape")
            )

            TachometerGauge(
                currentRpm = telemetry.rpm,
                elapsedTimeFormatted = telemetry.formattedElapsedTime,
                maxRpm = profile.maxRpm,
                size = 145.dp,
                modifier = Modifier.testTag("gauge_tachometer_landscape")
            )
        }

        // Coluna Central: Como no vídeo do carro (Cabeçalho de estatísticas + Carro e Faixa Animada)
        Column(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra superior com as 3 estatísticas como no vídeo do carro:
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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

            FaultBanner(
                diagnostics = diagnostics,
                onClick = onOpenDiagnostics
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConsumptionBlock(
                    telemetry = telemetry,
                    profile = profile,
                    modifier = Modifier.weight(1.2f).testTag("consumption_block_landscape")
                )
                DigitalFuelGauge(
                    telemetry = telemetry,
                    tankCapacity = profile.tankCapacityLiters.toFloat().takeIf { it > 10f } ?: 43.0f,
                    reserveThreshold = 9.0f,
                    modifier = Modifier.padding(start = 12.dp).testTag("digital_fuel_gauge_landscape")
                )
            }

            // Faixa de Energia com Carro e Setas Animadas (cor orientada pelo pedal do acelerador)
            val pedalLandscape = telemetry.sensors["pedal_d"] ?: telemetry.sensors["pedal_e"]
            val (accelLandscape, isPedalLandscape) = if (pedalLandscape != null && !pedalLandscape.isNaN()) {
                Pair(pedalLandscape, true)
            } else {
                Pair(telemetry.throttlePercent, false)
            }
            AnimatedEnergyRoad(
                driveMode = telemetry.driveMode,
                speedKmh = telemetry.speedKmh,
                accelPercent = accelLandscape,
                isPedal = isPedalLandscape,
                ecuLinkOk = telemetry.ecuLinkOk,
                modifier = Modifier.testTag("animated_energy_road_landscape")
            )

            Spacer(modifier = Modifier.height(2.dp))
        }

        // Coluna Direita: manômetros menores (toque para trocar o sensor)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SlotGauge(0, gaugeSlots, telemetry, 72.dp, onGaugeClick)
                SlotGauge(1, gaugeSlots, telemetry, 72.dp, onGaugeClick)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SlotGauge(2, gaugeSlots, telemetry, 72.dp, onGaugeClick)
                SlotGauge(3, gaugeSlots, telemetry, 72.dp, onGaugeClick)
            }

            if (profile.isHybrid) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SlotGauge(4, gaugeSlots, telemetry, 72.dp, onGaugeClick)
                }
            }
        }
    }
}

@Composable
private fun BatteryVoltageGauge(
    telemetry: TelemetryData,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.battery_voltage)
) {
    val v = telemetry.batteryVoltage
    MiniGauge(
        value = v,
        minValue = 0f,
        maxValue = 16f,
        label = label,
        unit = "V",
        centerType = MiniGaugeCenterType.NEEDLE_BATTERY,
        displayValue = if (v <= 0.1f) "--" else "%.1fV".format(v),
        size = size,
        modifier = modifier
    )
}

@Composable
private fun SocGauge(
    telemetry: TelemetryData,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.hybrid_soc)
) {
    MiniGauge(
        value = if (telemetry.hybridSocAvailable) telemetry.hybridBatterySocPercent else 0f,
        minValue = 0f,
        maxValue = 100f,
        label = label,
        unit = "%",
        centerType = MiniGaugeCenterType.NEEDLE_SOC,
        displayValue = if (telemetry.hybridSocAvailable) "${telemetry.hybridBatterySocPercent.toInt()}%" else "--",
        size = size,
        modifier = modifier
    )
}

/** Consumo médio (acima) e instantâneo (abaixo), centralizados. "~" = estimado pela tabela INMETRO. */
/**
 * Consumo médio e instantâneo lado a lado (retrato), alinhados aos manômetros de cima:
 * média sob o velocímetro, instantâneo sob o tacômetro. Mesma lógica de [ConsumptionBlock].
 */
@Composable
private fun ConsumptionRow(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    modifier: Modifier = Modifier
) {
    val prefix = if (telemetry.consumptionIsEstimated) "~" else ""
    val avgText = if (telemetry.avgConsumptionKmPerL <= 0f) "--"
    else prefix + "%.1f".format(telemetry.avgConsumptionKmPerL)

    val inst = telemetry.instantConsumptionKmPerL
    val instText = when {
        telemetry.speedKmh < 3f || inst <= 0f -> "--"
        inst >= 99f -> "∞"
        else -> prefix + "%.1f".format(inst)
    }
    val inmetro = InmetroConsumption.forProfile(profile)
    val reference = if (!telemetry.ethanolPercent.isNaN() && telemetry.ethanolPercent > 70f) inmetro.referenceEth else inmetro.referenceGas
    val instColor = when {
        instText == "--" -> TextMuted
        inst >= 99f -> GaugeGreen
        else -> InmetroConsumption.colorForConsumption(inst, reference)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        // Esquerda (sob o velocímetro): consumo médio
        Column(
            modifier = Modifier.width(152.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(avgText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.width(4.dp))
                Text("km/L", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(stringResource(R.string.avg_consumption), fontSize = 10.sp, color = TextMuted)
        }

        // Direita (sob o tacômetro): consumo instantâneo
        Column(
            modifier = Modifier.width(152.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(instText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = instColor)
                Spacer(modifier = Modifier.width(4.dp))
                Text("km/L", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(stringResource(R.string.instant_consumption), fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ConsumptionBlock(
    telemetry: TelemetryData,
    profile: VehicleProfile,
    modifier: Modifier = Modifier
) {
    val prefix = if (telemetry.consumptionIsEstimated) "~" else ""
    val avgText = if (telemetry.avgConsumptionKmPerL <= 0f) "--"
    else prefix + "%.1f".format(telemetry.avgConsumptionKmPerL)

    val inst = telemetry.instantConsumptionKmPerL
    val instText = when {
        telemetry.speedKmh < 3f || inst <= 0f -> "--"
        inst >= 99f -> "∞"
        else -> prefix + "%.1f".format(inst)
    }
    val inmetro = InmetroConsumption.forProfile(profile)
    val reference = if (!telemetry.ethanolPercent.isNaN() && telemetry.ethanolPercent > 70f) inmetro.referenceEth else inmetro.referenceGas
    val instColor = when {
        instText == "--" -> TextMuted
        inst >= 99f -> GaugeGreen
        else -> InmetroConsumption.colorForConsumption(inst, reference)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(avgText, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.width(4.dp))
            Text("km/L", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(stringResource(R.string.avg_consumption), fontSize = 10.sp, color = TextMuted)

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(instText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = instColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text("km/L", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text(stringResource(R.string.instant_consumption), fontSize = 10.sp, color = TextMuted)
    }
}

private fun Float.safe(fallback: Float) = if (isNaN()) fallback else this
private fun Float.degrees() = if (isNaN()) "--" else "${toInt()}°C"

/** Aviso de falha: aparece quando a luz de injeção acende ou a ECU registra códigos. */
@Composable
private fun FaultBanner(
    diagnostics: DiagnosticsState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!diagnostics.hasFaults) return
    val color = if (diagnostics.worstSeverity == DTCLevel.CRITICAL) GaugeRed else GaugeYellow
    val first = diagnostics.all.firstOrNull()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("fault_banner"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (diagnostics.milOn == true) "Luz de injeção acesa" else "Códigos de falha detectados",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (first != null) {
                Text(
                    text = "${first.code} - ${first.info.title}: ${first.info.description}",
                    fontSize = 11.sp,
                    color = TextWhite,
                    maxLines = 2
                )
                val others = diagnostics.all.size - 1
                if (others > 0) {
                    Text("+$others outro(s) código(s)", fontSize = 10.sp, color = TextMuted)
                }
            } else {
                Text(
                    text = "A ECU registrou ${diagnostics.reportedCount} código(s). Toque para ver o significado.",
                    fontSize = 11.sp,
                    color = TextWhite
                )
            }
        }
        Text("VER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
