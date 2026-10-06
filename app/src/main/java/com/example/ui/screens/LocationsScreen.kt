package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GarbagePointEntity
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertCriticalBg
import com.example.ui.theme.AlertCriticalRed
import com.example.ui.theme.CivicTealCollected
import kotlin.math.roundToInt

@Composable
fun LocationsScreen(
    garbagePoints: List<GarbagePointEntity>,
    searchQuery: String,
    selectedStatusFilter: String,
    selectedRiskFilter: String,
    sortByPriorityDesc: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onRiskFilterChange: (String) -> Unit,
    onTogglePrioritySort: () -> Unit,
    onSavePoint: (GarbagePointEntity) -> Unit,
    onDeletePoint: (GarbagePointEntity) -> Unit,
    onUpdateStatus: (GarbagePointEntity, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPoint by remember { mutableStateOf<GarbagePointEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var detailPoint by remember { mutableStateOf<GarbagePointEntity?>(null) }
    var deletingPoint by remember { mutableStateOf<GarbagePointEntity?>(null) }

    val filteredPoints = remember(
        garbagePoints,
        searchQuery,
        selectedStatusFilter,
        selectedRiskFilter,
        sortByPriorityDesc
    ) {
        garbagePoints.filter { pt ->
            val matchesQuery = searchQuery.isBlank() ||
                    pt.name.contains(searchQuery, ignoreCase = true) ||
                    pt.address.contains(searchQuery, ignoreCase = true) ||
                    pt.wasteType.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == "All" || pt.status == selectedStatusFilter
            val matchesRisk = selectedRiskFilter == "All" || pt.riskLevel == selectedRiskFilter
            matchesQuery && matchesStatus && matchesRisk
        }.let { list ->
            if (sortByPriorityDesc) {
                list.sortedByDescending { it.priorityScore }
            } else {
                list.sortedBy { it.priorityScore }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("locations_screen"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Search & Add Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search Rajkot locations...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("location_search_input"),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = { isAddingNew = true },
                modifier = Modifier
                    .height(46.dp)
                    .testTag("add_location_btn"),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Location",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Compact Filter Strip (Status + Risk + Priority Sort)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Sort Chip
            FilterChip(
                selected = sortByPriorityDesc,
                onClick = onTogglePrioritySort,
                label = {
                    Text(
                        text = if (sortByPriorityDesc) "Priority: High→Low" else "Priority: Low→High",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort by Priority",
                        modifier = Modifier.size(14.dp)
                    )
                }
            )

            listOf("All", "Pending", "Scheduled", "Collected").forEach { statusOpt ->
                FilterChip(
                    selected = selectedStatusFilter == statusOpt,
                    onClick = { onStatusFilterChange(statusOpt) },
                    label = {
                        Text(
                            text = if (statusOpt == "All") "Status: All" else statusOpt,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            listOf("All", "Critical", "High", "Medium", "Low").forEach { riskOpt ->
                FilterChip(
                    selected = selectedRiskFilter == riskOpt,
                    onClick = { onRiskFilterChange(riskOpt) },
                    label = {
                        Text(
                            text = if (riskOpt == "All") "Risk: All" else riskOpt,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }

        // 3. Count summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showing ${filteredPoints.size} of ${garbagePoints.size} Rajkot points",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap status chip to cycle • Tap card for details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 4. Locations List
        if (filteredPoints.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No matching locations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No Rajkot garbage points match the current search/filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("locations_lazy_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filteredPoints, key = { it.id }) { point ->
                    CompactLocationCard(
                        point = point,
                        onViewDetails = { detailPoint = point },
                        onEdit = { editingPoint = point },
                        onDelete = { deletingPoint = point },
                        onCycleStatus = { onUpdateStatus(point, null) },
                        onMarkCollected = { onUpdateStatus(point, "Collected") }
                    )
                }
            }
        }
    }

    // View Details Dialog
    detailPoint?.let { pt ->
        AlertDialog(
            onDismissRequest = { detailPoint = null },
            title = {
                Text(
                    text = pt.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RiskBadge(riskLevel = pt.riskLevel)
                        StatusBadge(status = pt.status)
                    }
                    Text("Address: ${pt.address}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Coordinates: ${pt.latitude}, ${pt.longitude}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Waste Type: ${pt.wasteType}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Load / Capacity: ${pt.estimatedWasteKg} kg / ${pt.binCapacityKg} kg (${pt.fillPercentage}% full)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Priority Score: ${pt.priorityScore}/100", style = MaterialTheme.typography.bodySmall)
                    if (pt.routeOrder > 0 && pt.status != "Collected") {
                        Text(
                            "Current Route Stop: #${pt.routeOrder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (pt.notes.isNotBlank()) {
                        Text("Notes: ${pt.notes}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val toEdit = pt
                    detailPoint = null
                    editingPoint = toEdit
                }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { detailPoint = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingPoint?.let { pt ->
        AlertDialog(
            onDismissRequest = { deletingPoint = null },
            title = {
                Text(
                    text = "Delete Collection Point?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove '${pt.name}' (${pt.address}) from Rajkot routes?",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePoint(pt)
                        deletingPoint = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCriticalRed),
                    modifier = Modifier.testTag("confirm_delete_location_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPoint = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Location Dialog
    if (isAddingNew || editingPoint != null) {
        LocationEditorDialog(
            initialPoint = editingPoint,
            onDismiss = {
                isAddingNew = false
                editingPoint = null
            },
            onSave = { saved ->
                onSavePoint(saved)
                isAddingNew = false
                editingPoint = null
            }
        )
    }
}

@Composable
private fun CompactLocationCard(
    point: GarbagePointEntity,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCycleStatus: () -> Unit,
    onMarkCollected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("location_card_${point.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                    if (point.routeOrder > 0 && point.status != "Collected") {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "#${point.routeOrder}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskBadge(riskLevel = point.riskLevel)
                    Box(modifier = Modifier.clickable { onCycleStatus() }) {
                        StatusBadge(status = point.status)
                    }
                }
            }

            Text(
                text = "${point.address} • ${point.wasteType}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waste: ${point.estimatedWasteKg}/${point.binCapacityKg} kg (${point.fillPercentage}%) • Priority: ${point.priorityScore}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (point.status != "Collected") {
                        TextButton(
                            onClick = onMarkCollected,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Collect",
                                tint = CivicTealCollected,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Collect",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTealCollected,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit location",
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
                            contentDescription = "Delete location",
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
private fun LocationEditorDialog(
    initialPoint: GarbagePointEntity?,
    onDismiss: () -> Unit,
    onSave: (GarbagePointEntity) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialPoint?.name ?: "") }
    var address by rememberSaveable { mutableStateOf(initialPoint?.address ?: "") }
    var latStr by rememberSaveable { mutableStateOf(initialPoint?.latitude?.toString() ?: "22.2850") }
    var lonStr by rememberSaveable { mutableStateOf(initialPoint?.longitude?.toString() ?: "70.7990") }
    var wasteType by rememberSaveable { mutableStateOf(initialPoint?.wasteType ?: "Mixed Municipal") }
    var estWasteStr by rememberSaveable { mutableStateOf(initialPoint?.estimatedWasteKg?.toString() ?: "350") }
    var capacityStr by rememberSaveable { mutableStateOf(initialPoint?.binCapacityKg?.toString() ?: "500") }
    var priorityStr by rememberSaveable { mutableStateOf(initialPoint?.priorityScore?.toString() ?: "75") }
    var riskLevel by rememberSaveable { mutableStateOf(initialPoint?.riskLevel ?: "High") }
    var status by rememberSaveable { mutableStateOf(initialPoint?.status ?: "Pending") }
    var notes by rememberSaveable { mutableStateOf(initialPoint?.notes ?: "") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val wasteTypes = listOf("Mixed Municipal", "Organic / Wet", "Recyclable Dry", "Commercial / Bulk")
    val riskLevels = listOf("Critical", "High", "Medium", "Low")
    val statuses = listOf("Pending", "Scheduled", "Collected")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialPoint == null) "Add Rajkot Garbage Point" else "Edit Garbage Point",
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
                if (validationError != null) {
                    Surface(
                        color = AlertCriticalBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertCriticalRed,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_location_name")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Area / Address in Rajkot *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_location_address")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Lat (22.15..22.45)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Lon (70.65..70.95)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = estWasteStr,
                        onValueChange = { estWasteStr = it },
                        label = { Text("Est. Waste (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capacityStr,
                        onValueChange = { capacityStr = it },
                        label = { Text("Bin Cap (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priorityStr,
                        onValueChange = { priorityStr = it },
                        label = { Text("Priority (1-100)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Waste Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    wasteTypes.forEach { wt ->
                        FilterChip(
                            selected = wasteType == wt,
                            onClick = { wasteType = wt },
                            label = { Text(wt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Risk Level:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    riskLevels.forEach { rl ->
                        FilterChip(
                            selected = riskLevel == rl,
                            onClick = { riskLevel = rl },
                            label = { Text(rl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Collection Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedAddr = address.trim()
                    val lat = latStr.toDoubleOrNull()
                    val lon = lonStr.toDoubleOrNull()
                    val waste = estWasteStr.toIntOrNull()
                    val cap = capacityStr.toIntOrNull()
                    val prio = priorityStr.toIntOrNull()

                    when {
                        trimmedName.isEmpty() -> validationError = "Location name is required."
                        trimmedAddr.isEmpty() -> validationError = "Address/area is required."
                        lat == null || lat !in 22.10..22.50 ->
                            validationError = "Enter a valid Rajkot latitude between 22.10 and 22.50."
                        lon == null || lon !in 70.60..71.00 ->
                            validationError = "Enter a valid Rajkot longitude between 70.60 and 71.00."
                        waste == null || waste < 0 -> validationError = "Estimated waste must be 0 or greater."
                        cap == null || cap <= 0 -> validationError = "Bin capacity must be greater than 0."
                        waste > cap * 2 -> validationError = "Estimated waste cannot exceed 200% of bin capacity."
                        prio == null || prio !in 1..100 -> validationError = "Priority score must be 1 to 100."
                        else -> {
                            val fillPct = ((waste.toDouble() / cap.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
                            onSave(
                                GarbagePointEntity(
                                    id = initialPoint?.id ?: 0,
                                    name = trimmedName,
                                    address = trimmedAddr,
                                    latitude = lat,
                                    longitude = lon,
                                    wasteType = wasteType,
                                    estimatedWasteKg = waste,
                                    binCapacityKg = cap,
                                    fillPercentage = if (status == "Collected") 0 else fillPct,
                                    priorityScore = prio,
                                    riskLevel = riskLevel,
                                    status = status,
                                    notes = notes.trim(),
                                    routeOrder = initialPoint?.routeOrder ?: 0
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_location_dialog_btn")
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
