package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CollectionLogEntity
import com.example.data.GarbagePointEntity
import com.example.data.RouteMetadataEntity
import com.example.data.VehicleEntity
import com.example.ui.components.CompactMetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatShortTimestamp
import com.example.ui.theme.AlertCriticalRed
import com.example.ui.theme.AlertHighOrangeRed
import com.example.ui.theme.AlertLowGreen
import com.example.ui.theme.AlertMediumOrange
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.CivicTealCollected
import com.example.ui.theme.RouteBlue
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    garbagePoints: List<GarbagePointEntity>,
    vehicles: List<VehicleEntity>,
    logs: List<CollectionLogEntity>,
    routeMetadata: RouteMetadataEntity?,
    modifier: Modifier = Modifier
) {
    val totalPoints = garbagePoints.size
    val collectedCount = garbagePoints.count { it.status == "Collected" }
    val scheduledCount = garbagePoints.count { it.status == "Scheduled" }
    val pendingOnlyCount = garbagePoints.count { it.status == "Pending" }
    val uncollectedCount = totalPoints - collectedCount

    val totalWasteCollectedKg = garbagePoints.filter { it.status == "Collected" }.sumOf { it.estimatedWasteKg }
    val totalPendingWasteKg = garbagePoints.filter { it.status != "Collected" }.sumOf { it.estimatedWasteKg }
    val grandTotalWasteKg = totalWasteCollectedKg + totalPendingWasteKg

    val routeDistanceKm = routeMetadata?.currentDistanceKm ?: 0.0
    val routeDurationMin = routeMetadata?.currentDurationMin ?: 0

    val activeFleet = vehicles.filter { it.isActive }
    val fleetCapKg = activeFleet.sumOf { it.payloadCapacityKg }
    val fleetLoadKg = activeFleet.sumOf { it.currentLoadKg }
    val fleetUtilPct = if (fleetCapKg > 0) {
        ((fleetLoadKg.toDouble() / fleetCapKg.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    } else 0

    // Risk level counts
    val critCount = garbagePoints.count { it.riskLevel == "Critical" }
    val highCount = garbagePoints.count { it.riskLevel == "High" }
    val medCount = garbagePoints.count { it.riskLevel == "Medium" }
    val lowCount = garbagePoints.count { it.riskLevel == "Low" }

    // Waste type distribution (by total kg)
    val wasteByType = garbagePoints.groupBy { it.wasteType }
        .mapValues { (_, pts) -> pts.sumOf { it.estimatedWasteKg } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Top Summary KPI Grid (6 metrics)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactMetricTile(
                        label = "Waste Collected",
                        value = "$totalWasteCollectedKg kg",
                        subValue = "Of $grandTotalWasteKg kg total",
                        icon = Icons.Default.Scale,
                        accentColor = CivicTealCollected,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Collection Points",
                        value = "$totalPoints",
                        subValue = "$collectedCount done / $uncollectedCount left",
                        icon = Icons.Default.LocationOn,
                        accentColor = CivicGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Fleet Utilization",
                        value = "$fleetUtilPct%",
                        subValue = "$fleetLoadKg / $fleetCapKg kg",
                        icon = Icons.Default.LocalShipping,
                        accentColor = AlertMediumOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactMetricTile(
                        label = "Route Distance",
                        value = "$routeDistanceKm km",
                        subValue = if (routeMetadata?.isOptimized == true) "Optimized path" else "Baseline path",
                        icon = Icons.Default.AltRoute,
                        accentColor = RouteBlue,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Estimated Time",
                        value = "$routeDurationMin min",
                        subValue = "Driving + bin load",
                        icon = Icons.Default.AccessTime,
                        accentColor = RouteBlue,
                        modifier = Modifier.weight(1f)
                    )
                    CompactMetricTile(
                        label = "Completion Rate",
                        value = "${if (totalPoints > 0) ((collectedCount * 100.0) / totalPoints).roundToInt() else 0}%",
                        subValue = "$collectedCount stops cleared",
                        icon = Icons.Default.CheckCircle,
                        accentColor = CivicTealCollected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Collected vs Pending / Scheduled Stacked Breakdown Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Collection Status Breakdown (Collected vs Pending)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val safeTotal = totalPoints.coerceAtLeast(1).toFloat()
                    val colFrac = (collectedCount / safeTotal).coerceIn(0f, 1f)
                    val schFrac = (scheduledCount / safeTotal).coerceIn(0f, 1f)
                    val penFrac = (pendingOnlyCount / safeTotal).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (colFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(colFrac)
                                    .fillMaxHeight()
                                    .background(CivicTealCollected)
                            )
                        }
                        if (schFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(schFrac)
                                    .fillMaxHeight()
                                    .background(RouteBlue)
                            )
                        }
                        if (penFrac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(penFrac)
                                    .fillMaxHeight()
                                    .background(AlertMediumOrange)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendStat(color = CivicTealCollected, label = "Collected: $collectedCount")
                        LegendStat(color = RouteBlue, label = "Scheduled: $scheduledCount")
                        LegendStat(color = AlertMediumOrange, label = "Pending: $pendingOnlyCount")
                    }
                }
            }
        }

        // 3. Risk Distribution & Waste Type Distribution Side-by-Side (or Stacked Compactly)
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Risk Level Distribution ($totalPoints Points)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    CompactHorizontalBarRow(
                        label = "Critical Risk",
                        countText = "$critCount pts",
                        fraction = if (totalPoints > 0) critCount.toFloat() / totalPoints else 0f,
                        barColor = AlertCriticalRed
                    )
                    CompactHorizontalBarRow(
                        label = "High Risk",
                        countText = "$highCount pts",
                        fraction = if (totalPoints > 0) highCount.toFloat() / totalPoints else 0f,
                        barColor = AlertHighOrangeRed
                    )
                    CompactHorizontalBarRow(
                        label = "Medium Risk",
                        countText = "$medCount pts",
                        fraction = if (totalPoints > 0) medCount.toFloat() / totalPoints else 0f,
                        barColor = AlertMediumOrange
                    )
                    CompactHorizontalBarRow(
                        label = "Low Risk",
                        countText = "$lowCount pts",
                        fraction = if (totalPoints > 0) lowCount.toFloat() / totalPoints else 0f,
                        barColor = AlertLowGreen
                    )
                }
            }
        }

        // 4. Waste Type Distribution Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Waste Type Distribution ($grandTotalWasteKg kg Total)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val safeWasteTotal = grandTotalWasteKg.coerceAtLeast(1).toFloat()
                    wasteByType.entries.sortedByDescending { it.value }.forEach { (wType, kg) ->
                        val pct = ((kg / safeWasteTotal) * 100).roundToInt()
                        CompactHorizontalBarRow(
                            label = wType,
                            countText = "$kg kg ($pct%)",
                            fraction = (kg / safeWasteTotal).coerceIn(0f, 1f),
                            barColor = CivicGreenPrimary
                        )
                    }
                }
            }
        }

        // 5. Vehicle Utilization Breakdown Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Vehicle Payload Utilization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    vehicles.forEach { v ->
                        val frac = if (v.payloadCapacityKg > 0) {
                            (v.currentLoadKg.toFloat() / v.payloadCapacityKg.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        CompactHorizontalBarRow(
                            label = "${v.name} (${v.fuelType})",
                            countText = "${v.currentLoadKg}/${v.payloadCapacityKg} kg",
                            fraction = frac,
                            barColor = if (v.isAssignedToRoute) CivicTealCollected else RouteBlue
                        )
                    }
                }
            }
        }

        // 6. Full Collection Activity Logs
        item {
            SectionHeader(
                title = "Collection Activity Log History",
                trailingText = "${logs.size} entries"
            )
        }

        items(logs, key = { "analytic_log_${it.id}" }) { log ->
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
                            contentDescription = "Log",
                            tint = CivicTealCollected,
                            modifier = Modifier.size(15.dp)
                        )
                        Column {
                            Text(
                                text = "${log.locationName} • ${log.wasteAmountKg} kg",
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

@Composable
private fun CompactHorizontalBarRow(
    label: String,
    countText: String,
    fraction: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun LegendStat(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
