package com.example.domain

import com.example.data.GarbagePointEntity
import com.example.data.SampleRajkotData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

data class RoadRouteResult(
    val distanceKm: Double,
    val durationMinutes: Int,
    val geometryPoints: List<LatLon>,
    val sourceLabel: String,
    val isFallback: Boolean,
    val warningMessage: String? = null
)

data class OptimizationComparison(
    val beforeDistanceKm: Double,
    val beforeDurationMin: Int,
    val beforeStopCount: Int,
    val afterDistanceKm: Double,
    val afterDurationMin: Int,
    val afterStopCount: Int,
    val orderedStops: List<GarbagePointEntity>,
    val skippedForCapacity: List<GarbagePointEntity>,
    val routeResult: RoadRouteResult
)

object RouteEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Calculates Haversine straight-line distance in kilometers between two coordinates.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Queries public OSRM API for real road routing across Rajkot streets.
     * Falls back cleanly if offline or if OSRM server is unreachable.
     */
    suspend fun fetchRoadRoute(
        startLat: Double,
        startLon: Double,
        stops: List<GarbagePointEntity>
    ): RoadRouteResult = withContext(Dispatchers.IO) {
        if (stops.isEmpty()) {
            return@withContext RoadRouteResult(
                distanceKm = 0.0,
                durationMinutes = 0,
                geometryPoints = listOf(LatLon(startLat, startLon)),
                sourceLabel = "No active stops",
                isFallback = false
            )
        }

        val allCoords = mutableListOf(LatLon(startLat, startLon))
        stops.forEach { allCoords.add(LatLon(it.latitude, it.longitude)) }

        val coordQuery = allCoords.joinToString(";") { "${it.lon},${it.lat}" }
        val url = "https://router.project-osrm.org/route/v1/driving/$coordQuery?overview=full&geometries=geojson"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GreenRoute-Rajkot-SmartCity/1.0 (GP-Rajkot)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext computeFallbackRoute(
                        allCoords = allCoords,
                        stopCount = stops.size,
                        reason = "OSRM HTTP ${response.code} - using Rajkot road-grid fallback"
                    )
                }

                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (json.optString("code") != "Ok") {
                    return@withContext computeFallbackRoute(
                        allCoords = allCoords,
                        stopCount = stops.size,
                        reason = "OSRM status ${json.optString("code")} - using fallback calculation"
                    )
                }

                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) {
                    return@withContext computeFallbackRoute(
                        allCoords = allCoords,
                        stopCount = stops.size,
                        reason = "No OSRM route returned - using fallback calculation"
                    )
                }

                val primaryRoute = routes.getJSONObject(0)
                val distanceMeters = primaryRoute.optDouble("distance", 0.0)
                val durationSeconds = primaryRoute.optDouble("duration", 0.0)

                val geometryObj = primaryRoute.getJSONObject("geometry")
                val coordsArray = geometryObj.getJSONArray("coordinates")
                val polyPoints = mutableListOf<LatLon>()
                for (i in 0 until coordsArray.length()) {
                    val pair = coordsArray.getJSONArray(i)
                    val lon = pair.getDouble(0)
                    val lat = pair.getDouble(1)
                    polyPoints.add(LatLon(lat, lon))
                }

                val distKm = ((distanceMeters / 1000.0) * 10.0).roundToInt() / 10.0
                // Municipal garbage truck: driving duration + 4 mins loading time per stop
                val driveMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                val totalMinutes = driveMinutes + (stops.size * 4)

                return@withContext RoadRouteResult(
                    distanceKm = distKm,
                    durationMinutes = totalMinutes,
                    geometryPoints = if (polyPoints.isNotEmpty()) polyPoints else allCoords,
                    sourceLabel = "OSRM Live Road Network",
                    isFallback = false,
                    warningMessage = null
                )
            }
        } catch (e: Exception) {
            return@withContext computeFallbackRoute(
                allCoords = allCoords,
                stopCount = stops.size,
                reason = "Routing service unreachable (${e.javaClass.simpleName}). Using offline urban road-factor calculation."
            )
        }
    }

    /**
     * Sensible road-grid fallback when offline or OSRM is temporarily unreachable.
     * Applies 1.28x Rajkot urban road winding factor + intermediate street-grid interpolation
     * and realistic 22 km/h municipal truck speed + 4 min per bin loading.
     */
    fun computeFallbackRoute(
        allCoords: List<LatLon>,
        stopCount: Int,
        reason: String
    ): RoadRouteResult {
        var totalHaversine = 0.0
        val interpolatedPath = mutableListOf<LatLon>()

        for (i in 0 until allCoords.size - 1) {
            val a = allCoords[i]
            val b = allCoords[i + 1]
            totalHaversine += haversineKm(a.lat, a.lon, b.lat, b.lon)
            interpolatedPath.add(a)
            // Create a realistic elbow waypoint along Rajkot's arterial E-W / N-S grid when offline
            val midLat = a.lat + (b.lat - a.lat) * 0.65
            val midLon = a.lon + (b.lon - a.lon) * 0.35
            interpolatedPath.add(LatLon(midLat, midLon))
        }
        if (allCoords.isNotEmpty()) {
            interpolatedPath.add(allCoords.last())
        }

        val roadDistKm = ((totalHaversine * 1.28) * 10.0).roundToInt() / 10.0
        val driveMinutes = ((roadDistKm / 22.0) * 60.0).roundToInt().coerceAtLeast(1)
        val totalMinutes = driveMinutes + (stopCount * 4)

        return RoadRouteResult(
            distanceKm = roadDistKm,
            durationMinutes = totalMinutes,
            geometryPoints = interpolatedPath,
            sourceLabel = "Offline Urban Road Fallback",
            isFallback = true,
            warningMessage = reason
        )
    }

    /**
     * Real Capacity-Aware, Priority-Weighted Nearest-Neighbor + 2-Opt Route Optimization Algorithm.
     * Considers:
     * 1. Vehicle start location (Depot)
     * 2. Garbage point coordinates
     * 3. Waste amount & available vehicle capacity
     * 4. Risk level & priority score
     * 5. Road/geographic distance
     */
    fun computeOptimizedSequence(
        startLat: Double = SampleRajkotData.DEPOT_LAT,
        startLon: Double = SampleRajkotData.DEPOT_LON,
        candidates: List<GarbagePointEntity>,
        availableVehicleCapacityKg: Int
    ): Pair<List<GarbagePointEntity>, List<GarbagePointEntity>> {
        val activeCandidates = candidates.filter { it.status != "Collected" }.toMutableList()
        if (activeCandidates.isEmpty()) return Pair(emptyList(), emptyList())

        val selectedOrdered = mutableListOf<GarbagePointEntity>()
        val skippedCapacity = mutableListOf<GarbagePointEntity>()

        var remainingCap = availableVehicleCapacityKg.coerceAtLeast(500)
        var currentLat = startLat
        var currentLon = startLon

        while (activeCandidates.isNotEmpty()) {
            // Find candidate that fits remaining capacity with minimum composite cost
            val fitting = activeCandidates.filter { it.estimatedWasteKg <= remainingCap }
            if (fitting.isEmpty()) {
                // Remaining stops exceed vehicle capacity
                skippedCapacity.addAll(activeCandidates)
                break
            }

            val nextStop = fitting.minByOrNull { point ->
                val distKm = haversineKm(currentLat, currentLon, point.latitude, point.longitude)
                val riskWeight = when (point.riskLevel) {
                    "Critical" -> 0.48
                    "High" -> 0.70
                    "Medium" -> 0.88
                    else -> 1.10
                }
                val priorityFactor = 1.0 - ((point.priorityScore.coerceIn(1, 100) / 100.0) * 0.32)
                distKm * riskWeight * priorityFactor
            }!!

            selectedOrdered.add(nextStop)
            activeCandidates.remove(nextStop)
            remainingCap -= nextStop.estimatedWasteKg
            currentLat = nextStop.latitude
            currentLon = nextStop.longitude
        }

        // Apply 2-Opt local search refinement to eliminate route crossovers while preserving Critical early stops
        val refined = applyTwoOptRefinement(startLat, startLon, selectedOrdered)
        return Pair(refined, skippedCapacity)
    }

    private fun applyTwoOptRefinement(
        startLat: Double,
        startLon: Double,
        route: List<GarbagePointEntity>
    ): List<GarbagePointEntity> {
        if (route.size < 4) return route
        val best = route.toMutableList()

        fun routeCost(list: List<GarbagePointEntity>): Double {
            var total = haversineKm(startLat, startLon, list.first().latitude, list.first().longitude)
            for (i in 0 until list.size - 1) {
                total += haversineKm(
                    list[i].latitude, list[i].longitude,
                    list[i + 1].latitude, list[i + 1].longitude
                )
            }
            // Small penalty if a Critical stop is pushed past index 4
            list.forEachIndexed { idx, pt ->
                if (pt.riskLevel == "Critical" && idx > 3) {
                    total += (idx - 3) * 0.45
                }
            }
            return total
        }

        var improved = true
        var iterations = 0
        while (improved && iterations < 20) {
            improved = false
            iterations++
            val currentBestScore = routeCost(best)
            for (i in 0 until best.size - 2) {
                for (k in i + 1 until best.size) {
                    val candidate = best.toMutableList()
                    candidate.subList(i, k + 1).reverse()
                    if (routeCost(candidate) + 0.05 < currentBestScore) {
                        for (idx in best.indices) best[idx] = candidate[idx]
                        improved = true
                    }
                }
            }
        }
        return best
    }

    fun encodeGeometryToJson(points: List<LatLon>): String {
        val arr = JSONArray()
        points.forEach { pt ->
            val pair = JSONArray()
            pair.put(pt.lat)
            pair.put(pt.lon)
            arr.put(pair)
        }
        return arr.toString()
    }

    fun decodeGeometryFromJson(jsonStr: String): List<LatLon> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<LatLon>()
            for (i in 0 until arr.length()) {
                val pair = arr.getJSONArray(i)
                list.add(LatLon(pair.getDouble(0), pair.getDouble(1)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
