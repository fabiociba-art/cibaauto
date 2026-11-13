package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.PreloadedVehicles
import com.example.model.VehicleProfile
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitCardBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.GaugeYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun ProfileEditDialog(
    profile: VehicleProfile,
    onDismiss: () -> Unit,
    onSave: (VehicleProfile) -> Unit
) {
    var brand by remember { mutableStateOf(profile.brand) }
    var model by remember { mutableStateOf(profile.model) }
    var version by remember { mutableStateOf(profile.version) }
    var year by remember { mutableStateOf(profile.year) }
    var engine by remember { mutableStateOf(profile.engine) }
    var displacement by remember { mutableStateOf(profile.displacement) }
    var fuelType by remember { mutableStateOf(profile.fuelType) }
    var horsepower by remember { mutableStateOf(profile.horsepower) }
    var torque by remember { mutableStateOf(profile.torque) }
    var transmission by remember { mutableStateOf(profile.transmission) }
    var drivetrain by remember { mutableStateOf(profile.drivetrain) }
    var tankCapacity by remember { mutableStateOf(profile.tankCapacity) }
    var isHybrid by remember { mutableStateOf(profile.isHybrid) }
    var notes by remember { mutableStateOf(profile.notes) }

    val originalProfile = remember(profile.id) {
        PreloadedVehicles.all.firstOrNull { it.id == profile.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .testTag("dialog_profile_edit"),
        containerColor = CockpitCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GaugeGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = GaugeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Editar Perfil do Veículo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = profile.name,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                if (originalProfile != null) {
                    TextButton(
                        onClick = {
                            brand = originalProfile.brand
                            model = originalProfile.model
                            version = originalProfile.version
                            year = originalProfile.year
                            engine = originalProfile.engine
                            displacement = originalProfile.displacement
                            fuelType = originalProfile.fuelType
                            horsepower = originalProfile.horsepower
                            torque = originalProfile.torque
                            transmission = originalProfile.transmission
                            drivetrain = originalProfile.drivetrain
                            tankCapacity = originalProfile.tankCapacity
                            isHybrid = originalProfile.isHybrid
                            notes = originalProfile.notes
                        },
                        modifier = Modifier.testTag("btn_restore_original_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restaurar original",
                            tint = GaugeYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar", color = GaugeYellow, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = GaugeGreen,
                unfocusedBorderColor = CockpitCardBorder,
                focusedLabelColor = GaugeGreen,
                unfocusedLabelColor = TextMuted,
                cursorColor = GaugeGreen
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Marca e Modelo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_brand")
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_model")
                    )
                }

                // Versão e Ano
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Versão") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1.3f).testTag("input_version")
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Ano") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        modifier = Modifier.weight(0.7f).testTag("input_year")
                    )
                }

                // Motor e Cilindrada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = engine,
                        onValueChange = { engine = it },
                        label = { Text("Motorização") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1.2f).testTag("input_engine")
                    )
                    OutlinedTextField(
                        value = displacement,
                        onValueChange = { displacement = it },
                        label = { Text("Cilindrada") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(0.8f).testTag("input_displacement")
                    )
                }

                // Capacidade do Tanque (L)
                OutlinedTextField(
                    value = tankCapacity,
                    onValueChange = { tankCapacity = it },
                    label = { Text("Capacidade do Tanque (L ou texto)") },
                    placeholder = { Text("Ex: 50 litros") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = GaugeGreen
                        )
                    },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("input_tank_capacity")
                )

                // Tipo de Combustível
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("Tipo de Combustível") },
                    placeholder = { Text("Flex (Etanol / Gasolina), Gasolina, Diesel...") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("input_fuel_type")
                )

                // Potência e Torque
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = horsepower,
                        onValueChange = { horsepower = it },
                        label = { Text("Potência (cv)") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_horsepower")
                    )
                    OutlinedTextField(
                        value = torque,
                        onValueChange = { torque = it },
                        label = { Text("Torque (kgfm)") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_torque")
                    )
                }

                // Câmbio e Tração
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = transmission,
                        onValueChange = { transmission = it },
                        label = { Text("Câmbio") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_transmission")
                    )
                    OutlinedTextField(
                        value = drivetrain,
                        onValueChange = { drivetrain = it },
                        label = { Text("Tração") },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f).testTag("input_drivetrain")
                    )
                }

                // Card de Motorização Híbrida
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isHybrid) GaugeGreen else CockpitCardBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHybrid) Color(0xFF132A1C) else CockpitSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = if (isHybrid) GaugeGreen else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Motorização Híbrida (HEV / PHEV)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHybrid) GaugeGreen else TextWhite
                                )
                                Text(
                                    text = "Habilita telemetria e fluxo de energia elétrico/combustão",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Switch(
                            checked = isHybrid,
                            onCheckedChange = { isHybrid = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CockpitBlack,
                                checkedTrackColor = GaugeGreen
                            ),
                            modifier = Modifier.testTag("switch_hybrid_profile")
                        )
                    }
                }

                // Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações adicionais") },
                    colors = textFieldColors,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            brand = brand.trim(),
                            model = model.trim(),
                            version = version.trim(),
                            year = year.trim(),
                            engine = engine.trim(),
                            displacement = displacement.trim(),
                            fuelType = fuelType.trim(),
                            horsepower = horsepower.trim(),
                            torque = torque.trim(),
                            transmission = transmission.trim(),
                            drivetrain = drivetrain.trim(),
                            tankCapacity = tankCapacity.trim(),
                            isHybrid = isHybrid,
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GaugeGreen,
                    contentColor = CockpitBlack
                ),
                modifier = Modifier.testTag("btn_save_profile")
            ) {
                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CockpitCardBorder)
                ),
                modifier = Modifier.testTag("btn_cancel_profile")
            ) {
                Text("Cancelar")
            }
        }
    )
}
