package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.GarbagePointEntity
import com.example.data.RouteMetadataEntity
import com.example.data.SampleRajkotData
import com.example.domain.LatLon
import com.example.ui.components.RiskBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AlertCriticalRed
import com.example.ui.theme.AlertHighOrangeRed
import com.example.ui.theme.AlertLowGreen
import com.example.ui.theme.AlertMediumOrange
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.CivicTealCollected
import com.example.ui.theme.RouteBlue
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun LiveMapScreen(
    garbagePoints: List<GarbagePointEntity>,
    routeGeometry: List<LatLon>,
    routeMetadata: RouteMetadataEntity?,
    isRoutingLoading: Boolean,
    onOptimizeNow: () -> Unit,
    onRefreshRoadRoute: () -> Unit,
    onUpdatePointStatus: (GarbagePointEntity, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPoint by remember { mutableStateOf<GarbagePointEntity?>(null) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    // Configure osmdroid user agent safely
    remember {
        Configuration.getInstance().userAgentValue = context.packageName.ifBlank { "com.aistudio.greenroute.rjktgp" }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.2)
            controller.setCenter(GeoPoint(22.2850, 70.7990)) // Rajkot center
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_map_screen")
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()

                // 1. Draw Real Road Polyline
                if (routeGeometry.size >= 2) {
                    val roadLine = Polyline().apply {
                        setPoints(routeGeometry.map { GeoPoint(it.lat, it.lon) })
                        outlinePaint.color = AndroidColor.parseColor("#1565C0")
                        outlinePaint.strokeWidth = 11f
                        outlinePaint.isAntiAlias = true
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    map.overlays.add(roadLine)
                }

                // 2. Draw Start Depot Marker (Government Polytechnic Rajkot / RMC Depot)
                val depotLat = routeMetadata?.startLat ?: SampleRajkotData.DEPOT_LAT
                val depotLon = routeMetadata?.startLon ?: SampleRajkotData.DEPOT_LON
                val depotMarker = Marker(map).apply {
                    position = GeoPoint(depotLat, depotLon)
                    title = routeMetadata?.startName ?: SampleRajkotData.DEPOT_NAME
                    snippet = "Vehicle Start & Dispatch Depot"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createNumberedMarkerDrawable(
                        context = context,
                        label = "S",
                        bgColorHex = "#1B5E20",
                        sizeDp = 30
                    )
                }
                map.overlays.add(depotMarker)

                // 3. Draw Garbage Collection Markers (Numbered when in route, Colored by Risk/Status)
                garbagePoints.forEach { point ->
                    val markerColorHex = when {
                        point.status == "Collected" -> "#00796B" // Teal/green
                        point.riskLevel == "Critical" -> "#D32F2F" // Red
                        point.riskLevel == "High" -> "#E64A19"     // Orange-red
                        point.riskLevel == "Medium" -> "#F57C00"   // Orange
                        else -> "#388E3C"                          // Low = Green
                    }
                    val badgeText = if (point.status != "Collected" && point.routeOrder > 0) {
                        point.routeOrder.toString()
                    } else if (point.status == "Collected") {
                        "✓"
                    } else {
                        "•"
                    }

                    val marker = Marker(map).apply {
                        position = GeoPoint(point.latitude, point.longitude)
                        title = if (point.routeOrder > 0 && point.status != "Collected") {
                            "Stop #${point.routeOrder}: ${point.name}"
                        } else {
                            point.name
                        }
                        snippet = "${point.riskLevel} Risk • ${point.fillPercentage}% Full (${point.estimatedWasteKg} kg) • ${point.status}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createNumberedMarkerDrawable(
                            context = context,
                            label = badgeText,
                            bgColorHex = markerColorHex,
                            sizeDp = 28
                        )
                        setOnMarkerClickListener { m, _ ->
                            selectedPoint = point
                            m.showInfoWindow()
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            }
        )

        // Recenter trigger effect
        if (recenterTrigger > 0) {
            mapView.controller.animateTo(GeoPoint(22.2850, 70.7990), 13.2, 400L)
        }

        // Compact Top Overlay Strip (Distance, Time, Stops, Actions)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Rajkot Live Route",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${routeMetadata?.currentDistanceKm ?: 0.0} km • ${routeMetadata?.currentDurationMin ?: 0} min • ${routeMetadata?.currentStopCount ?: 0} stops",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRoutingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        IconButton(
                            onClick = { recenterTrigger++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Recenter Rajkot",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRefreshRoadRoute,
                            enabled = !isRoutingLoading,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recalculate Road Route",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Button(
                            onClick = onOptimizeNow,
                            enabled = !isRoutingLoading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("map_optimize_btn"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Optimize",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Optimize",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Compact Color Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = CivicGreenPrimary, label = "S: Depot")
                    LegendDot(color = AlertCriticalRed, label = "Critical")
                    LegendDot(color = AlertHighOrangeRed, label = "High")
                    LegendDot(color = AlertMediumOrange, label = "Med")
                    LegendDot(color = AlertLowGreen, label = "Low")
                    LegendDot(color = CivicTealCollected, label = "Collected")
                    LegendDot(color = RouteBlue, label = "Road Route")
                }
            }
        }

        // Compact Selected Marker Card at Bottom (only when a marker is tapped)
        selectedPoint?.let { currentPt ->
            // Read latest state from garbagePoints list in case status changed
            val livePoint = garbagePoints.find { it.id == currentPt.id } ?: currentPt
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
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
                            if (livePoint.routeOrder > 0 && livePoint.status != "Collected") {
                                Surface(
                                    color = RouteBlue,
                                    shape = CircleShape,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${livePoint.routeOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = livePoint.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { selectedPoint = null },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close stop details",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "${livePoint.address} • ${livePoint.wasteType}",
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RiskBadge(riskLevel = livePoint.riskLevel)
                            StatusBadge(status = livePoint.status)
                            Text(
                                text = "${livePoint.estimatedWasteKg}/${livePoint.binCapacityKg} kg (${livePoint.fillPercentage}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (livePoint.status != "Collected") {
                            Button(
                                onClick = { onUpdatePointStatus(livePoint, "Collected") },
                                colors = ButtonDefaults.buttonColors(containerColor = CivicTealCollected),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark Collected",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mark Collected",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onUpdatePointStatus(livePoint, "Pending") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Reopen Stop",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Generates a crisp, numbered circular BitmapDrawable for osmdroid markers.
 */
private fun createNumberedMarkerDrawable(
    context: Context,
    label: String,
    bgColorHex: String,
    sizeDp: Int
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val px = (sizeDp * density).toInt().coerceAtLeast(36)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(bgColorHex)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 11.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val radius = px / 2f - (1.5f * density)
    canvas.drawCircle(px / 2f, px / 2f, radius, fillPaint)
    canvas.drawCircle(px / 2f, px / 2f, radius, strokePaint)

    val textY = (px / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(label, px / 2f, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
