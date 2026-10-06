package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CollectionLogEntity
import com.example.data.GarbagePointEntity
import com.example.data.RouteMetadataEntity
import com.example.data.VehicleEntity
import com.example.ui.AppTab
import com.example.ui.components.CompactMetricTile
import com.example.ui.components.RiskBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatShortTimestamp
import com.example.ui.theme.AlertCriticalBg
import com.example.ui.theme.AlertCriticalRed
import com.example.ui.theme.AlertHighOrangeRed
import com.example.ui.theme.AlertMediumOrange
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.CivicTealCollected
import com.example.ui.theme.RouteBlue
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    garbagePoints: List<GarbagePointEntity>,
    vehicles: List<VehicleEntity>,
    logs: List<CollectionLogEntity>,
    routeMetadata: RouteMetadataEntity?,
    onNavigateTab: (AppTab) -> Unit,
    onQuickMarkCollected: (GarbagePointEntity) -> Unit,
    onOptimizeNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPoints = garbagePoints.size
    val collectedPoints = garbagePoints.count { it.status == "Collected" }
    val pendingPoints = garbagePoints.count { it.status != "Collected" }
    val criticalPointsList = garbagePoints.filter { it.riskLevel == "Critical" && it.status != "Collected" }
    val criticalCount = criticalPointsList.size

    val totalDistanceKm = routeMetadata?.currentDistanceKm ?: 0.0
    val estTimeMin = routeMetadata?.currentDurationMin ?: 0

    val activeVehicles = vehicles.filter { it.isActive }
    val totalFleetCapacityKg = activeVehicles.sumOf { it.payloadCapacityKg }
    val currentCollectedWasteKg = garbagePoints.filter { it.status == "Collected" }.sumOf { it.estimatedWasteKg }
    val totalScheduledWasteKg = garbagePoints.sumOf { it.estimatedWasteKg }

    val progressFraction = if (totalPoints > 0) {
        collectedPoints.toFloat() / totalPoints.toFloat()
    } else 0f
    val progressPercent = (progressFraction * 100).roundToInt()

    val assignedVehicle = vehicles.firstOrNull { it.isAssignedToRoute && it.isActive }
        ?: activeVehicles.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Today's Collection Progress Bar Card (Compact at very top)
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Rajkot Collection Progress",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$collectedPoints / $totalPoints Stops ($progressPercent%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CivicTealCollected,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collected Waste: $currentCollectedWasteKg / $totalScheduledWasteKg kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = assignedVehicle?.let { "Active Unit: ${it.name}" } ?: "No Vehicle Assigned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Compact 4x2 KPI Grid (8 core metrics requested)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactMetricTile(
                        label = "Total Points",
                        value = "$totalPoints",
                        subValue = "Rajkot Zones",
                        icon = Icons.Default.LocationOn,
                        accentColor = CivicGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Pending Stops",
                        value = "$pendingPoints",
                        subValue = "Awaiting pickup",
                        icon = Icons.Default.PendingActions,
                        accentColor = AlertMediumOrange,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Collected Today",
                        value = "$collectedPoints",
                        subValue = "$progressPercent% cleared",
                        icon = Icons.Default.CheckCircle,
                        accentColor = CivicTealCollected,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Critical Overflow",
                        value = "$criticalCount",
                        subValue = if (criticalCount > 0) "Urgent action" else "All clear",
                        icon = Icons.Default.Warning,
                        accentColor = AlertCriticalRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactMetricTile(
                        label = "Route Distance",
                        value = "$totalDistanceKm km",
                        subValue = if (routeMetadata?.isOptimized == true) "Optimized" else "Unoptimized",
                        icon = Icons.Default.AltRoute,
                        accentColor = RouteBlue,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Est. Route Time",
                        value = "$estTimeMin min",
                        subValue = "${routeMetadata?.currentStopCount ?: pendingPoints} active stops",
                        icon = Icons.Default.AccessTime,
                        accentColor = RouteBlue,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Fleet Capacity",
                        value = "$totalFleetCapacityKg kg",
                        subValue = "${activeVehicles.size} active trucks",
                        icon = Icons.Default.LocalShipping,
                        accentColor = CivicGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Collected Waste",
                        value = "$currentCollectedWasteKg kg",
                        subValue = "Today's load",
                        icon = Icons.Default.Scale,
                        accentColor = CivicTealCollected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Compact Route Summary & Quick Action Bar
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Route Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Start: ${routeMetadata?.startName ?: "RMC Depot (GP Rajkot)"} • ${routeMetadata?.routingSource ?: "OSRM Road Network"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = if (routeMetadata?.isOptimized == true) CivicGreenPrimary.copy(alpha = 0.12f)
                            else AlertMediumOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (routeMetadata?.isOptimized == true) "OPTIMIZED" else "STANDARD ORDER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (routeMetadata?.isOptimized == true) CivicGreenPrimary else AlertHighOrangeRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onOptimizeNow()
                                onNavigateTab(AppTab.OPTIMIZE)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("dashboard_optimize_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Optimize",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Optimize Route",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { onNavigateTab(AppTab.LIVE_MAP) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("dashboard_map_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "View Map",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Open Live Map",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 4. Critical Overflow Alerts Section (Compact)
        item {
            SectionHeader(
                title = "Critical Overflow Alerts (${criticalPointsList.size})",
                trailingText = "Tap Collect when cleared"
            )
        }

        if (criticalPointsList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "No critical alerts",
                            tint = CivicTealCollected,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "No critical overflow bins pending in Rajkot right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(criticalPointsList, key = { "crit_${it.id}" }) { point ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertCriticalBg.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, AlertCriticalRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = point.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                RiskBadge(riskLevel = point.riskLevel)
                            }
                            Text(
                                text = "${point.address} • Fill: ${point.fillPercentage}% (${point.estimatedWasteKg}/${point.binCapacityKg} kg)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { onQuickMarkCollected(point) },
                            colors = ButtonDefaults.buttonColors(containerColor = CivicTealCollected),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Collect",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Recent Collection Activity Logs (Top 5)
        item {
            SectionHeader(
                title = "Recent Collection Logs",
                trailingText = "${logs.size} recorded"
            )
        }

        val recentLogs = logs.take(5)
        if (recentLogs.isEmpty()) {
            item {
                Text(
                    text = "No collection activity logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp)
                )
            }
        } else {
            items(recentLogs, key = { "log_${it.id}" }) { log ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Log entry",
                                tint = CivicTealCollected,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "${log.locationName} (${log.wasteAmountKg} kg)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${log.assignedVehicle} • ${formatShortTimestamp(log.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        StatusBadge(status = log.status)
                    }
                }
            }
        }
    }
}
