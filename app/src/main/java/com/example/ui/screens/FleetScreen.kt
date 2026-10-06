package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VehicleEntity
import com.example.ui.theme.AlertCriticalBg
import com.example.ui.theme.AlertCriticalRed
import com.example.ui.theme.AlertMediumOrange
import com.example.ui.theme.CivicGreenLight
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.CivicTealCollected
import kotlin.math.roundToInt

@Composable
fun FleetScreen(
    vehicles: List<VehicleEntity>,
    onSaveVehicle: (VehicleEntity) -> Unit,
    onDeleteVehicle: (VehicleEntity) -> Unit,
    onAssignToRoute: (VehicleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddingVehicle by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    var deletingVehicle by remember { mutableStateOf<VehicleEntity?>(null) }

    val activeCount = vehicles.count { it.isActive }
    val totalCapacity = vehicles.filter { it.isActive }.sumOf { it.payloadCapacityKg }
    val totalCurrentLoad = vehicles.sumOf { it.currentLoadKg }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("fleet_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Compact Fleet Summary Header Bar
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RMC Municipal Fleet (${vehicles.size} Vehicles)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active: $activeCount • Total Cap: $totalCapacity kg • Loaded: $totalCurrentLoad kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { isAddingVehicle = true },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_vehicle_btn"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Vehicle",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Vehicle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Compact Vehicle Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(vehicles, key = { it.id }) { vehicle ->
                CompactVehicleCard(
                    vehicle = vehicle,
                    onAssign = { onAssignToRoute(vehicle) },
                    onEdit = { editingVehicle = vehicle },
                    onDelete = { deletingVehicle = vehicle }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    deletingVehicle?.let { v ->
        AlertDialog(
            onDismissRequest = { deletingVehicle = null },
            title = {
                Text(
                    text = "Remove Municipal Vehicle?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete '${v.name}' (${v.registrationNumber}) from the Rajkot fleet database?",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVehicle(v)
                        deletingVehicle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCriticalRed),
                    modifier = Modifier.testTag("confirm_delete_vehicle_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVehicle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Vehicle Dialog
    if (isAddingVehicle || editingVehicle != null) {
        VehicleEditorDialog(
            initialVehicle = editingVehicle,
            onDismiss = {
                isAddingVehicle = false
                editingVehicle = null
            },
            onSave = { saved ->
                onSaveVehicle(saved)
                isAddingVehicle = false
                editingVehicle = null
            }
        )
    }
}

@Composable
private fun CompactVehicleCard(
    vehicle: VehicleEntity,
    onAssign: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val loadFraction = if (vehicle.payloadCapacityKg > 0) {
        (vehicle.currentLoadKg.toFloat() / vehicle.payloadCapacityKg.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val loadPct = (loadFraction * 100).roundToInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_card_${vehicle.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (vehicle.isAssignedToRoute) 1.4.dp else 0.8.dp,
            color = if (vehicle.isAssignedToRoute) CivicGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Vehicle",
                        tint = if (vehicle.isActive) CivicGreenPrimary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${vehicle.name} (${vehicle.registrationNumber})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = vehicle.fuelType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (vehicle.isActive) CivicGreenLight else AlertCriticalBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (vehicle.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicle.isActive) CivicGreenPrimary else AlertCriticalRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Driver & Load info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Driver: ${vehicle.driverName} (${vehicle.driverContact})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Load: ${vehicle.currentLoadKg}/${vehicle.payloadCapacityKg} kg ($loadPct%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { loadFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (loadPct > 85) AlertMediumOrange else CivicTealCollected,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vehicle.isAssignedToRoute) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Assigned",
                            tint = CivicGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Assigned to Active Route",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAssign,
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("assign_vehicle_${vehicle.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Assign to Route",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Vehicle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Vehicle",
                            tint = AlertCriticalRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleEditorDialog(
    initialVehicle: VehicleEntity?,
    onDismiss: () -> Unit,
    onSave: (VehicleEntity) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialVehicle?.name ?: "") }
    var regNo by rememberSaveable { mutableStateOf(initialVehicle?.registrationNumber ?: "GJ-03-") }
    var fuelType by rememberSaveable { mutableStateOf(initialVehicle?.fuelType ?: "CNG") }
    var capacityStr by rememberSaveable { mutableStateOf(initialVehicle?.payloadCapacityKg?.toString() ?: "4000") }
    var currentLoadStr by rememberSaveable { mutableStateOf(initialVehicle?.currentLoadKg?.toString() ?: "0") }
    var driverName by rememberSaveable { mutableStateOf(initialVehicle?.driverName ?: "") }
    var driverContact by rememberSaveable { mutableStateOf(initialVehicle?.driverContact ?: "+91 ") }
    var isActive by rememberSaveable { mutableStateOf(initialVehicle?.isActive ?: true) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    val fuelOptions = listOf("CNG", "Electric EV", "Diesel")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialVehicle == null) "Add Collection Vehicle" else "Edit Vehicle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (errorMsg != null) {
                    Surface(
                        color = AlertCriticalBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertCriticalRed,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_vehicle_name")
                )

                OutlinedTextField(
                    value = regNo,
                    onValueChange = { regNo = it },
                    label = { Text("Registration Number (e.g. GJ-03-GA-1234) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_vehicle_reg")
                )

                Text("Fuel Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fuelOptions.forEach { ft ->
                        FilterChip(
                            selected = fuelType == ft,
                            onClick = { fuelType = ft },
                            label = { Text(ft, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = capacityStr,
                        onValueChange = { capacityStr = it },
                        label = { Text("Payload Cap (kg) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentLoadStr,
                        onValueChange = { currentLoadStr = it },
                        label = { Text("Current Load (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text("Driver Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_vehicle_driver")
                )

                OutlinedTextField(
                    value = driverContact,
                    onValueChange = { driverContact = it },
                    label = { Text("Driver Contact *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vehicle Active Status", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedReg = regNo.trim()
                    val cap = capacityStr.toIntOrNull()
                    val load = currentLoadStr.toIntOrNull() ?: 0
                    val trimmedDriver = driverName.trim()
                    val trimmedPhone = driverContact.trim()

                    when {
                        trimmedName.isEmpty() -> errorMsg = "Vehicle name is required."
                        trimmedReg.length < 5 -> errorMsg = "Valid registration number is required."
                        cap == null || cap < 500 -> errorMsg = "Payload capacity must be at least 500 kg."
                        load < 0 || load > cap -> errorMsg = "Current load must be between 0 and $cap kg."
                        trimmedDriver.isEmpty() -> errorMsg = "Driver name is required."
                        trimmedPhone.length < 8 -> errorMsg = "Valid driver contact number is required."
                        else -> {
                            onSave(
                                VehicleEntity(
                                    id = initialVehicle?.id ?: 0,
                                    name = trimmedName,
                                    registrationNumber = trimmedReg,
                                    fuelType = fuelType,
                                    payloadCapacityKg = cap,
                                    currentLoadKg = load,
                                    driverName = trimmedDriver,
                                    driverContact = trimmedPhone,
                                    isActive = isActive,
                                    isAssignedToRoute = initialVehicle?.isAssignedToRoute ?: false
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_vehicle_dialog_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
