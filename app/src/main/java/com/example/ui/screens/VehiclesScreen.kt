package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PreloadedVehicles
import com.example.model.VehicleProfile
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun VehiclesScreen(
    vehicles: List<VehicleProfile>,
    selectedVehicle: VehicleProfile,
    onSelectVehicle: (VehicleProfile) -> Unit,
    onSaveVehicle: (VehicleProfile) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit = {}
) {
  BackHandler { onBack() }

  var editingVehicle by remember { mutableStateOf<VehicleProfile?>(null) }
  var isCreatingNew by remember { mutableStateOf(false) }
  var vehicleToDelete by remember { mutableStateOf<VehicleProfile?>(null) }
  var showResetConfirmation by remember { mutableStateOf(false) }

  // Busca e filtro por marca / modelo
  var searchQuery by remember { mutableStateOf("") }
  var selectedBrand by remember { mutableStateOf<String?>(null) }

  val brands = remember(vehicles) {
    vehicles.map { it.brand.trim() }.filter { it.isNotBlank() }.distinct().sorted()
  }

  val filteredVehicles = remember(vehicles, searchQuery, selectedBrand) {
    val q = searchQuery.trim().lowercase()
    vehicles.filter { v ->
      val matchBrand = selectedBrand == null || v.brand.equals(selectedBrand, ignoreCase = true)
      val matchSearch = q.isEmpty() ||
          v.brand.lowercase().contains(q) ||
          v.model.lowercase().contains(q) ||
          v.version.lowercase().contains(q) ||
          v.year.lowercase().contains(q) ||
          v.fullName.lowercase().contains(q) ||
          v.engine.lowercase().contains(q)
      matchBrand && matchSearch
    }
  }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(CockpitBlack)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
        }
        Column {
          Text(
              text = "GARAGEM & CALIBRAÇÃO OBD-2",
              color = ElectricBlue,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp
          )
          Text(
              text = "Perfis de Veículos",
              color = TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
          )
        }
      }

      Button(
          onClick = {
            editingVehicle = VehicleProfile(
                id = UUID.randomUUID().toString(),
                brand = "",
                model = "",
                version = "",
                year = "2025",
                engine = "Motor Flex",
                displacement = "2.0L",
                fuelType = "Flex (Etanol / Gasolina)",
                horsepower = "170 cv",
                torque = "21.0 kgfm",
                transmission = "Automático / CVT",
                drivetrain = "Tração Dianteira (FWD)",
                tankCapacity = "50 litros",
                isHybrid = false,
                notes = ""
            )
            isCreatingNew = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("btn_new_vehicle")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = CockpitBlack, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Novo Veículo", color = CockpitBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    // Explanation Banner
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CockpitSurfaceVariant,
        border = BorderStroke(1.dp, CockpitCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
      Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = "Por que os dados do perfil são essenciais?",
              color = TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
          )
          Text(
              text = "A cilindrada calcula o consumo Speed-Density (sem MAF), a capacidade do tanque define a autonomia (Range em KM) e o modo híbrido ativa a leitura da ECU 7E2.",
              color = TextSecondary,
              fontSize = 11.sp,
              lineHeight = 15.sp
          )
        }
      }
    }

    // Busca por marca / modelo
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_search"),
        placeholder = {
          Text("Buscar por marca, modelo, versão ou ano...", color = TextMuted, fontSize = 13.sp)
        },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = CockpitCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = ElectricBlue,
            focusedContainerColor = CockpitSurface,
            unfocusedContainerColor = CockpitSurface
        )
    )

    // Filtro por marca (chips)
    if (brands.isNotEmpty()) {
      LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
      ) {
        item {
          FilterChip(
              selected = selectedBrand == null,
              onClick = { selectedBrand = null },
              label = { Text("Todas", fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                  selectedLabelColor = ElectricBlue,
                  containerColor = CockpitSurfaceVariant,
                  labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selectedBrand == null,
                  borderColor = CockpitCardBorder,
                  selectedBorderColor = ElectricBlue
              )
          )
        }
        items(brands) { brand ->
          FilterChip(
              selected = selectedBrand == brand,
              onClick = {
                selectedBrand = if (selectedBrand == brand) null else brand
              },
              label = { Text(brand, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                  selectedLabelColor = ElectricBlue,
                  containerColor = CockpitSurfaceVariant,
                  labelColor = TextSecondary
              ),
              border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = selectedBrand == brand,
                  borderColor = CockpitCardBorder,
                  selectedBorderColor = ElectricBlue
              )
          )
        }
      }
    }

    // Contador de resultados
    Text(
        text = when {
          filteredVehicles.isEmpty() -> "Nenhum veículo encontrado"
          filteredVehicles.size == vehicles.size -> "${vehicles.size} veículos"
          else -> "${filteredVehicles.size} de ${vehicles.size} veículos"
        },
        color = TextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 2.dp)
    )

    // List of vehicles (filtrada)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(filteredVehicles, key = { it.id }) { vehicle ->
        val isSelected = vehicle.id == selectedVehicle.id

        VehicleCard(
            vehicle = vehicle,
            isSelected = isSelected,
            onSelect = { onSelectVehicle(vehicle) },
            onEdit = {
              editingVehicle = vehicle
              isCreatingNew = false
            },
            onDuplicate = {
              editingVehicle = vehicle.copy(
                  id = UUID.randomUUID().toString(),
                  version = if (vehicle.version.isNotBlank()) "${vehicle.version} (Cópia)" else "Cópia",
                  notes = "Cópia baseada em ${vehicle.fullName}."
              )
              isCreatingNew = true
            },
            onDelete = {
              vehicleToDelete = vehicle
            },
            canDelete = vehicles.size > 1
        )
      }

      if (filteredVehicles.isEmpty()) {
        item {
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nenhum perfil corresponde à busca.", color = TextSecondary, fontSize = 14.sp)
            if (searchQuery.isNotBlank() || selectedBrand != null) {
              TextButton(onClick = {
                searchQuery = ""
                selectedBrand = null
              }) {
                Text("Limpar filtros", color = ElectricBlue, fontSize = 13.sp)
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(
            onClick = { showResetConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CockpitCardBorder)
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Restaurar Perfis de Fábrica (Toyota)", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Dialog for Creating or Editing
  editingVehicle?.let { currentProfile ->
    VehicleEditorDialog(
        initialProfile = currentProfile,
        isNew = isCreatingNew,
        onDismiss = { editingVehicle = null },
        onSave = { updated ->
          onSaveVehicle(updated)
          editingVehicle = null
        }
    )
  }

  // Delete Confirmation Dialog
  vehicleToDelete?.let { toDelete ->
    AlertDialog(
        onDismissRequest = { vehicleToDelete = null },
        title = { Text(text = "Excluir Perfil de Veículo?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
          Text(
              text = "Tem certeza de que deseja remover \"${toDelete.fullName}\"? Esta ação não pode ser desfeita.",
              color = TextSecondary
          )
        },
        confirmButton = {
          Button(
              onClick = {
                onDeleteVehicle(toDelete.id)
                vehicleToDelete = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
          ) {
            Text("Excluir", color = CockpitBlack, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { vehicleToDelete = null }) {
            Text("Cancelar", color = TextSecondary)
          }
        },
        containerColor = CockpitSurface,
        shape = RoundedCornerShape(16.dp)
    )
  }

  // Reset to Defaults Confirmation Dialog
  if (showResetConfirmation) {
    AlertDialog(
        onDismissRequest = { showResetConfirmation = false },
        title = { Text(text = "Restaurar Perfis de Fábrica?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
          Text(
              text = "Todos os perfis personalizados serão substituídos pelos 4 modelos padrão da Toyota (Corolla GLi Hybrid 2026, Corolla Altis 2.0, Corolla Hybrid 1.8 e Yaris 1.5). Deseja continuar?",
              color = TextSecondary
          )
        },
        confirmButton = {
          Button(
              onClick = {
                onResetDefaults()
                showResetConfirmation = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
          ) {
            Text("Restaurar", color = CockpitBlack, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showResetConfirmation = false }) {
            Text("Cancelar", color = TextSecondary)
          }
        },
        containerColor = CockpitSurface,
        shape = RoundedCornerShape(16.dp)
    )
  }
}

@Composable
fun VehicleCard(
    vehicle: VehicleProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
  Surface(
      shape = RoundedCornerShape(16.dp),
      color = if (isSelected) CockpitSurfaceVariant else CockpitSurface,
      border = BorderStroke(1.dp, if (isSelected) ElectricBlue else CockpitCardBorder),
      modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelect() }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top row: Icon, Name, and Status Badges
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
          Box(
              modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(if (vehicle.isHybrid) StatusGreen.copy(alpha = 0.15f) else ElectricBlue.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
          ) {
            Icon(
                imageVector = if (vehicle.isHybrid) Icons.Default.Bolt else Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (vehicle.isHybrid) StatusGreen else ElectricBlue,
                modifier = Modifier.size(22.dp)
            )
          }

          Column {
            Text(
                text = vehicle.fullName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = vehicle.engine,
                color = TextSecondary,
                fontSize = 12.sp
            )
          }
        }

        if (isSelected) {
          Surface(
              shape = RoundedCornerShape(8.dp),
              color = ElectricBlue
          ) {
            Text(
                text = "ATIVO",
                color = CockpitBlack,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = CockpitCardBorder)
      Spacer(modifier = Modifier.height(10.dp))

      // Critical Specs Grid
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
        VehicleSpecItem(
            label = "Cilindrada (Speed-Density)",
            value = "${String.format("%.3f", vehicle.getDisplacementLiters())} L",
            badge = "Consumo"
        )
        VehicleSpecItem(
            label = "Tanque (Autonomia)",
            value = "${vehicle.getTankCapacityLiters().toInt()} Litros",
            badge = "Range"
        )
        VehicleSpecItem(
            label = "Combustível",
            value = if (vehicle.fuelType.contains("Flex")) "Flex (E/G)" else vehicle.fuelType.take(12),
            badge = "AFR"
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
        VehicleSpecItem(
            label = "Câmbio",
            value = vehicle.transmission.take(18)
        )
        VehicleSpecItem(
            label = "Potência / Torque",
            value = "${vehicle.horsepower.substringBefore(" ").take(7)} / ${vehicle.torque.substringBefore(" ").take(7)}"
        )
        VehicleSpecItem(
            label = "Tecnologia",
            value = if (vehicle.isHybrid) "⚡ Híbrido 7E2" else "⛽ Combustão"
        )
      }

      if (vehicle.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Obs: ${vehicle.notes}",
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = CockpitCardBorder)
      Spacer(modifier = Modifier.height(10.dp))

      // Card Actions Row
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Edit Button
          FilledTonalButton(
              onClick = onEdit,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = CockpitSurface,
                  contentColor = NeonCyan
              ),
              border = BorderStroke(1.dp, CockpitCardBorder)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Editar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // Duplicate Button
          FilledTonalButton(
              onClick = onDuplicate,
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = CockpitSurface,
                  contentColor = TextSecondary
              ),
              border = BorderStroke(1.dp, CockpitCardBorder)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Duplicar", fontSize = 11.sp)
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir Perfil", tint = StatusRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
          }

          if (!isSelected) {
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
              Text(text = "Selecionar", color = CockpitBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun VehicleSpecItem(label: String, value: String, badge: String? = null) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = label, color = TextSecondary, fontSize = 9.sp)
      if (badge != null) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = ElectricBlue.copy(alpha = 0.2f)
        ) {
          Text(text = badge, color = ElectricBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
        }
      }
    }
    Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}

/**
 * Editor Completo com todos os campos necessários para o correto funcionamento do aplicativo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditorDialog(
    initialProfile: VehicleProfile,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (VehicleProfile) -> Unit
) {
  var brand by remember { mutableStateOf(initialProfile.brand) }
  var model by remember { mutableStateOf(initialProfile.model) }
  var version by remember { mutableStateOf(initialProfile.version) }
  var year by remember { mutableStateOf(initialProfile.year) }
  var engine by remember { mutableStateOf(initialProfile.engine) }
  var displacement by remember { mutableStateOf(initialProfile.displacement) }
  var fuelType by remember { mutableStateOf(initialProfile.fuelType) }
  var horsepower by remember { mutableStateOf(initialProfile.horsepower) }
  var torque by remember { mutableStateOf(initialProfile.torque) }
  var transmission by remember { mutableStateOf(initialProfile.transmission) }
  var drivetrain by remember { mutableStateOf(initialProfile.drivetrain) }
  var tankCapacity by remember { mutableStateOf(initialProfile.tankCapacity) }
  var isHybrid by remember { mutableStateOf(initialProfile.isHybrid) }
  var notes by remember { mutableStateOf(initialProfile.notes) }

  var validationError by remember { mutableStateOf<String?>(null) }

  // Live computed telemetry parameters
  val tempProfile = VehicleProfile(
      id = initialProfile.id,
      brand = brand,
      model = model,
      version = version,
      year = year,
      engine = engine,
      displacement = displacement,
      fuelType = fuelType,
      horsepower = horsepower,
      torque = torque,
      transmission = transmission,
      drivetrain = drivetrain,
      tankCapacity = tankCapacity,
      isHybrid = isHybrid,
      notes = notes
  )

  val computedDisplacementL = tempProfile.getDisplacementLiters()
  val computedTankL = tempProfile.getTankCapacityLiters()

  val performSave: () -> Unit = {
    if (brand.isBlank() || model.isBlank()) {
      validationError = "Por favor, preencha ao menos Marca e Modelo do veículo."
    } else {
      val finalProfile = VehicleProfile(
          id = initialProfile.id,
          brand = brand.trim(),
          model = model.trim(),
          version = version.trim(),
          year = year.trim(),
          engine = engine.ifBlank { "Motor Flex" }.trim(),
          displacement = displacement.ifBlank { "2.0L" }.trim(),
          fuelType = fuelType.ifBlank { "Flex (Etanol / Gasolina)" }.trim(),
          horsepower = horsepower.trim(),
          torque = torque.trim(),
          transmission = transmission.ifBlank { "Automático" }.trim(),
          drivetrain = drivetrain.ifBlank { "Tração Dianteira (FWD)" }.trim(),
          tankCapacity = tankCapacity.ifBlank { "50 litros" }.trim(),
          isHybrid = isHybrid,
          notes = notes.trim()
      )
      onSave(finalProfile)
    }
  }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(
          usePlatformDefaultWidth = false,
          decorFitsSystemWindows = false
      )
  ) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .imePadding(),
        shape = RoundedCornerShape(20.dp),
        color = CockpitBlack,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
    ) {
      Column(
          modifier = Modifier.fillMaxSize()
      ) {
        // Modal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
          ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
              Icon(
                  if (isNew) Icons.Default.Add else Icons.Default.Edit,
                  contentDescription = null,
                  tint = ElectricBlue,
                  modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                  text = if (isNew) "CRIAR NOVO PERFIL" else "EDITAR PERFIL DE VEÍCULO",
                  color = ElectricBlue,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
              )
              Text(
                  text = if (isNew) "Novo Veículo" else tempProfile.fullName.ifBlank { "Configuração de Perfil" },
                  color = TextPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
              )
            }
          }

          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Quick Save Button on Top Header
            Button(
                onClick = performSave,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_top_save_vehicle")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, tint = CockpitBlack, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Salvar", color = CockpitBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
            }
          }
        }

        HorizontalDivider(color = CockpitCardBorder)

        // Scrollable Form Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
          // Banner explicativo dos campos necessários
          Surface(
              shape = RoundedCornerShape(12.dp),
              color = CockpitSurface,
              border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "Campos Necessários para o Funcionamento do App",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
              }
              Text(
                  text = "Preencha os campos abaixo com precisão. O algoritmo de telemetria OBD-2 utiliza a Cilindrada para calcular vazão de combustível (L/h e km/L sem MAF), a Capacidade do Tanque para estimar a Autonomia restante (km) e a opção Híbrido para acionar a ECU 7E2 da Toyota.",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  lineHeight = 15.sp
              )
            }
          }

          // Seção 1: IDENTIFICAÇÃO DO VEÍCULO
          EditorSection(
              title = "1. IDENTIFICAÇÃO BÁSICA",
              subtitle = "Exibido no painel, histórico e no Android Auto",
              icon = Icons.Default.DirectionsCar
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                  value = brand,
                  onValueChange = { brand = it; validationError = null },
                  label = { Text("Marca * (ex: Toyota, Honda)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
              OutlinedTextField(
                  value = model,
                  onValueChange = { model = it; validationError = null },
                  label = { Text("Modelo * (ex: Corolla, Yaris)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                  value = version,
                  onValueChange = { version = it },
                  label = { Text("Versão (ex: Altis Premium, XEi)") },
                  modifier = Modifier.weight(1.3f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
              OutlinedTextField(
                  value = year,
                  onValueChange = { year = it },
                  label = { Text("Ano (ex: 2025)") },
                  modifier = Modifier.weight(0.7f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
            }
          }

          // Seção 2: TELEMETRIA CRÍTICA OBD-2 (CONSUMO, RANGE & HÍBRIDO)
          EditorSection(
              title = "2. PARÂMETROS CRÍTICOS DE TELEMETRIA (OBD-2)",
              subtitle = "Usados pelos algoritmos físicos de cálculo em tempo real",
              icon = Icons.Default.Calculate,
              badgeText = "CRUCIAL PARA OBD"
          ) {
            // Cilindrada do Motor
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                  value = displacement,
                  onValueChange = { displacement = it },
                  label = { Text("Cilindrada do Motor * (ex: 2.0L ou 1.987 cm³)") },
                  supportingText = {
                    Text(
                        text = "Valor computado: ${String.format("%.3f", computedDisplacementL)} Litros. Usado na fórmula Speed-Density (MAP x IAT x RPM / Cilindrada) quando não há MAF.",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )

              // Quick displacement presets
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("1.0L", "1.5L", "1.8L", "2.0L", "2.5L").forEach { preset ->
                  AssistChip(
                      onClick = { displacement = preset },
                      label = { Text(preset, fontSize = 10.sp) },
                      colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary),
                      border = BorderStroke(0.5.dp, CockpitCardBorder)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Capacidade do Tanque
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedTextField(
                  value = tankCapacity,
                  onValueChange = { tankCapacity = it },
                  label = { Text("Capacidade do Tanque * (ex: 50 litros ou 43L)") },
                  supportingText = {
                    Text(
                        text = "Capacidade interpretada: ${computedTankL.toInt()} Litros. Multiplicado pelo % da boia (PID 012F) para calcular Autonomia (Range em KM).",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )

              // Quick tank presets
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("43L", "45L", "50L", "55L", "60L").forEach { preset ->
                  AssistChip(
                      onClick = { tankCapacity = preset },
                      label = { Text(preset, fontSize = 10.sp) },
                      colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary),
                      border = BorderStroke(0.5.dp, CockpitCardBorder)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Switch Híbrido
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CockpitSurfaceVariant,
                border = BorderStroke(1.dp, if (isHybrid) StatusGreen.copy(alpha = 0.6f) else CockpitCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                  Icon(
                      Icons.Default.Bolt,
                      contentDescription = null,
                      tint = if (isHybrid) StatusGreen else TextSecondary,
                      modifier = Modifier.size(24.dp)
                  )
                  Column {
                    Text(
                        text = "Veículo Híbrido (Toyota THS II / Hybrid Synergy)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Habilita consultas na ECU 7E2/21CE para monitorar bateria HV (%) e potência do motor elétrico (kW).",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                  }
                }
                Switch(
                    checked = isHybrid,
                    onCheckedChange = { isHybrid = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StatusGreen,
                        checkedTrackColor = StatusGreen.copy(alpha = 0.3f)
                    )
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tipo de Combustível (Razão AFR Estequiométrica)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                  text = "Tipo de Combustível (Calibração da razão ar/combustível AFR):",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
              )
              val fuelOptions = listOf(
                  "Flex (Etanol / Gasolina)",
                  "Gasolina Pura",
                  "Etanol Puro",
                  "Híbrido Flex",
                  "Diesel"
              )
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                fuelOptions.take(3).forEach { option ->
                  val selected = fuelType == option
                  FilterChip(
                      selected = selected,
                      onClick = { fuelType = option },
                      label = { Text(option.substringBefore(" "), fontSize = 10.sp) },
                      colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = ElectricBlue,
                          selectedLabelColor = CockpitBlack
                      )
                  )
                }
              }
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                fuelOptions.drop(3).forEach { option ->
                  val selected = fuelType == option
                  FilterChip(
                      selected = selected,
                      onClick = { fuelType = option },
                      label = { Text(option, fontSize = 10.sp) },
                      colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = ElectricBlue,
                          selectedLabelColor = CockpitBlack
                      )
                  )
                }
              }

              OutlinedTextField(
                  value = fuelType,
                  onValueChange = { fuelType = it },
                  label = { Text("Descrição do Combustível") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
            }

            // Nome técnico do motor
            OutlinedTextField(
                value = engine,
                onValueChange = { engine = it },
                label = { Text("Nome / Código do Motor (ex: 2.0L Dynamic Force Dual VVT-iE)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = editorTextFieldColors()
            )
          }

          // Seção 3: ESPECIFICAÇÕES DE TRANSMISSÃO & DESEMPENHO
          EditorSection(
              title = "3. TRANSMISSÃO & DESEMPENHO",
              subtitle = "Parâmetros para medição de potência e ficha técnica",
              icon = Icons.Default.Speed
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                  value = horsepower,
                  onValueChange = { horsepower = it },
                  label = { Text("Potência (ex: 175 cv)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
              OutlinedTextField(
                  value = torque,
                  onValueChange = { torque = it },
                  label = { Text("Torque (ex: 21,3 kgfm)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                  value = transmission,
                  onValueChange = { transmission = it },
                  label = { Text("Transmissão (ex: Direct Shift CVT)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
              OutlinedTextField(
                  value = drivetrain,
                  onValueChange = { drivetrain = it },
                  label = { Text("Tração (ex: FWD, AWD)") },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = editorTextFieldColors()
              )
            }
          }

          // Seção 4: NOTAS TÉCNICAS E CALIBRAÇÕES ESPECÍFICAS
          EditorSection(
              title = "4. NOTAS TÉCNICAS & CALIBRAÇÕES",
              subtitle = "Anotações adicionais, injeção, remap ou modificações",
              icon = Icons.Default.Description
          ) {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações (opcional)") },
                placeholder = { Text("Ex: Injeção direta D-4S, filtro esportivo, calibração especial...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = editorTextFieldColors()
            )
          }

          // Error banner if any
          if (validationError != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusRed.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, StatusRed)
            ) {
              Text(
                  text = validationError ?: "",
                  color = StatusRed,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(10.dp)
              )
            }
          }
        }

        // Pinned Bottom Sticky Action Bar: Always visible above navigation bars
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CockpitSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, CockpitCardBorder)
        ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CockpitCardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
              Text(text = "Cancelar", color = TextSecondary, fontSize = 14.sp)
            }

            Button(
                onClick = performSave,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("btn_save_vehicle")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, tint = CockpitBlack, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                  text = if (isNew) "Salvar Novo" else "Salvar Alterações",
                  color = CockpitBlack,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun EditorSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
  Surface(
      shape = RoundedCornerShape(14.dp),
      color = CockpitSurface,
      border = BorderStroke(1.dp, CockpitCardBorder),
      modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
          Column {
            Text(text = title, color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp)
          }
        }
        if (badgeText != null) {
          Surface(
              shape = RoundedCornerShape(6.dp),
              color = ElectricBlue.copy(alpha = 0.2f),
              border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
          ) {
            Text(
                text = badgeText,
                color = ElectricBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      HorizontalDivider(color = CockpitCardBorder.copy(alpha = 0.5f))
      content()
    }
  }
}

@Composable
fun editorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricBlue,
    unfocusedBorderColor = CockpitCardBorder,
    focusedLabelColor = ElectricBlue,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = ElectricBlue
)
