package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GarbagePointEntity
import com.example.data.RouteMetadataEntity
import com.example.data.SampleRajkotData
import com.example.data.VehicleEntity
import com.example.ui.AppTab
import com.example.ui.components.RiskBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertHighOrangeRed
import com.example.ui.theme.AlertMediumBg
import com.example.ui.theme.CivicGreenLight
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.CivicTealCollected
import com.example.ui.theme.RouteBlue
import kotlin.math.roundToInt

@Composable
fun OptimizeScreen(
    orderedStops: List<GarbagePointEntity>,
    routeMetadata: RouteMetadataEntity?,
    vehicles: List<VehicleEntity>,
    skippedCapacityNames: List<String>,
    isRoutingLoading: Boolean,
    onRunOptimization: () -> Unit,
    onMoveStop: (Int, Boolean) -> Unit,
    onMarkStopCollected: (GarbagePointEntity) -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val beforeDist = routeMetadata?.beforeDistanceKm ?: 0.0
    val beforeTime = routeMetadata?.beforeDurationMin ?: 0
    val beforeStops = routeMetadata?.beforeStopCount ?: orderedStops.size

    val afterDist = routeMetadata?.currentDistanceKm ?: 0.0
    val afterTime = routeMetadata?.currentDurationMin ?: 0
    val afterStops = routeMetadata?.currentStopCount ?: orderedStops.size

    val distSavedKm = ((beforeDist - afterDist) * 10.0).roundToInt() / 10.0
    val timeSavedMin = beforeTime - afterTime
    val distSavedPct = if (beforeDist > 0.1 && distSavedKm > 0) {
        ((distSavedKm / beforeDist) * 100.0).roundToInt()
    } else 0

    val assignedVehicle = vehicles.firstOrNull { it.isAssignedToRoute && it.isActive }
        ?: vehicles.firstOrNull { it.isActive }

    val totalRouteWasteKg = orderedStops.sumOf { it.estimatedWasteKg }
    val availableCapKg = assignedVehicle?.let { (it.payloadCapacityKg - it.currentLoadKg).coerceAtLeast(0) } ?: 4500

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("optimize_screen"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Algorithm & Action Header Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                                text = "Priority-Weighted Nearest-Neighbor + 2-Opt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Depot: ${SampleRajkotData.DEPOT_NAME} • Cap: $totalRouteWasteKg / $availableCapKg kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isRoutingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunOptimization,
                            enabled = !isRoutingLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("run_optimization_btn"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Calculate Optimized Route",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRoutingLoading) "Calculating..." else "Calculate Optimized Route",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToMap,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("optimize_view_map_btn"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "View on Map",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Map",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. BEFORE vs AFTER Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                        Text(
                            text = "Route Comparison (Before vs After)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = routeMetadata?.routingSource ?: "OSRM Road Network",
                            style = MaterialTheme.typography.labelSmall,
                            color = RouteBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // BEFORE Column
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "BEFORE (INITIAL)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$beforeDist km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Est. Time: $beforeTime min",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Stops: $beforeStops points",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // AFTER Column
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = CivicGreenLight.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CivicGreenPrimary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "AFTER (CURRENT)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicGreenPrimary
                                )
                                Text(
                                    text = "$afterDist km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicGreenPrimary
                                )
                                Text(
                                    text = "Est. Time: $afterTime min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CivicGreenPrimary
                                )
                                Text(
                                    text = "Stops: $afterStops points",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CivicGreenPrimary
                                )
                            }
                        }
                    }

                    // Calculated Savings Bar
                    Surface(
                        color = if (distSavedKm > 0) CivicGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Savings",
                                    tint = CivicGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (distSavedKm > 0) {
                                        "Savings: $distSavedKm km ($distSavedPct% shorter) • ${timeSavedMin.coerceAtLeast(0)} min saved"
                                    } else if (routeMetadata?.isOptimized == true) {
                                        "Route is at optimal sequence for current stops"
                                    } else {
                                        "Tap 'Calculate Optimized Route' to compute road savings"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicGreenPrimary
                                )
                            }
                        }
                    }

                    if (skippedCapacityNames.isNotEmpty()) {
                        Surface(
                            color = AlertMediumBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Deferred due to vehicle payload capacity: ${skippedCapacityNames.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertHighOrangeRed,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Start Depot Indicator
        item {
            SectionHeader(
                title = "Ordered Route Sequence (${orderedStops.size} Stops)",
                trailingText = "Use ↑ / ↓ to reorder & recalculate"
            )
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.8.dp, CivicGreenPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = CivicGreenPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Start Depot",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Start Point: ${SampleRajkotData.DEPOT_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Coordinates: ${SampleRajkotData.DEPOT_LAT}, ${SampleRajkotData.DEPOT_LON}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Numbered Route Stops with Manual Up/Down Reordering
        if (orderedStops.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "All garbage points have been collected! Add or reopen locations to build a route.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            itemsIndexed(orderedStops, key = { _, stop -> "route_stop_${stop.id}" }) { index, stop ->
                val isFirst = index == 0
                val isLast = index == orderedStops.lastIndex

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("route_stop_card_${index + 1}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stop Number Circle + Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = RouteBlue,
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stop.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RiskBadge(riskLevel = stop.riskLevel)
                                }
                                Text(
                                    text = "${stop.address} • ${stop.estimatedWasteKg} kg (${stop.fillPercentage}% full) • Prio ${stop.priorityScore}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Up / Down Manual Reorder + Quick Collect Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onMoveStop(stop.id, true) },
                                enabled = !isFirst && !isRoutingLoading,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("move_up_${stop.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Stop Up",
                                    tint = if (!isFirst && !isRoutingLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveStop(stop.id, false) },
                                enabled = !isLast && !isRoutingLoading,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("move_down_${stop.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Stop Down",
                                    tint = if (!isLast && !isRoutingLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMarkStopCollected(stop) },
                                enabled = !isRoutingLoading,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("collect_stop_${stop.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark Stop Collected",
                                    tint = CivicTealCollected,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
