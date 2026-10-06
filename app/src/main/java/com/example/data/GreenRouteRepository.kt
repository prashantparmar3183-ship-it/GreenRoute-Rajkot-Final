package com.example.data

import com.example.domain.LatLon
import com.example.domain.OptimizationComparison
import com.example.domain.RoadRouteResult
import com.example.domain.RouteEngine
import kotlinx.coroutines.flow.Flow

class GreenRouteRepository(private val dao: GreenRouteDao) {

    val garbagePointsFlow: Flow<List<GarbagePointEntity>> = dao.getAllGarbagePointsFlow()
    val vehiclesFlow: Flow<List<VehicleEntity>> = dao.getAllVehiclesFlow()
    val logsFlow: Flow<List<CollectionLogEntity>> = dao.getAllLogsFlow()
    val routeMetadataFlow: Flow<RouteMetadataEntity?> = dao.getRouteMetadataFlow()

    /**
     * Seeds realistic Rajkot data ONLY when the database is initially empty.
     * Never resets user data on subsequent launches.
     */
    suspend fun ensureSeededAndInitialized() {
        val pointCount = dao.getGarbagePointCount()
        if (pointCount == 0) {
            val initialPoints = SampleRajkotData.getInitialGarbagePoints()
            dao.insertAllGarbagePoints(initialPoints)
        }

        val vehicleCount = dao.getVehicleCount()
        if (vehicleCount == 0) {
            val initialVehicles = SampleRajkotData.getInitialVehicles()
            dao.insertAllVehicles(initialVehicles)
            val initialLogs = SampleRajkotData.getInitialLogs()
            initialLogs.forEach { dao.insertLog(it) }
        }

        val existingMeta = dao.getRouteMetadataOnce()
        if (existingMeta == null) {
            val allPoints = dao.getAllGarbagePointsOnce()
            val activeStops = allPoints
                .filter { it.status != "Collected" && it.routeOrder > 0 }
                .sortedBy { it.routeOrder }

            val initialRoute = RouteEngine.fetchRoadRoute(
                startLat = SampleRajkotData.DEPOT_LAT,
                startLon = SampleRajkotData.DEPOT_LON,
                stops = activeStops
            )

            val meta = RouteMetadataEntity(
                id = 1,
                isOptimized = false,
                beforeDistanceKm = initialRoute.distanceKm,
                beforeDurationMin = initialRoute.durationMinutes,
                beforeStopCount = activeStops.size,
                currentDistanceKm = initialRoute.distanceKm,
                currentDurationMin = initialRoute.durationMinutes,
                currentStopCount = activeStops.size,
                routingSource = initialRoute.sourceLabel,
                encodedPolylineJson = RouteEngine.encodeGeometryToJson(initialRoute.geometryPoints),
                startName = SampleRajkotData.DEPOT_NAME,
                startLat = SampleRajkotData.DEPOT_LAT,
                startLon = SampleRajkotData.DEPOT_LON,
                lastUpdated = System.currentTimeMillis()
            )
            dao.saveRouteMetadata(meta)
        }
    }

    suspend fun addOrUpdateGarbagePoint(point: GarbagePointEntity) {
        if (point.id == 0) {
            val currentPoints = dao.getAllGarbagePointsOnce()
            val maxOrder = currentPoints.maxOfOrNull { it.routeOrder } ?: 0
            val orderToAssign = if (point.status != "Collected") maxOrder + 1 else 0
            dao.insertGarbagePoint(point.copy(routeOrder = orderToAssign))
        } else {
            dao.updateGarbagePoint(point)
        }
        recalculateCurrentRouteMetrics(preserveBaseline = true)
    }

    suspend fun deleteGarbagePoint(point: GarbagePointEntity) {
        dao.deleteGarbagePoint(point)
        normalizeRouteOrdersAndRecalculate()
    }

    suspend fun updatePointStatus(point: GarbagePointEntity, newStatus: String) {
        val previousStatus = point.status
        if (previousStatus == newStatus) return

        val vehicles = dao.getAllVehiclesOnce()
        val assignedVehicle = vehicles.firstOrNull { it.isAssignedToRoute && it.isActive }
            ?: vehicles.firstOrNull { it.isActive }

        val updatedOrder = if (newStatus == "Collected") 0 else {
            if (point.routeOrder > 0) point.routeOrder else {
                val maxOrder = dao.getAllGarbagePointsOnce().maxOfOrNull { it.routeOrder } ?: 0
                maxOrder + 1
            }
        }

        val updatedFill = if (newStatus == "Collected") 0 else point.fillPercentage
        val updatedPoint = point.copy(
            status = newStatus,
            routeOrder = updatedOrder,
            fillPercentage = updatedFill
        )
        dao.updateGarbagePoint(updatedPoint)

        // Update vehicle current load & create collection log when marked Collected
        if (newStatus == "Collected") {
            if (assignedVehicle != null) {
                val newLoad = (assignedVehicle.currentLoadKg + point.estimatedWasteKg)
                    .coerceAtMost(assignedVehicle.payloadCapacityKg)
                dao.updateVehicle(assignedVehicle.copy(currentLoadKg = newLoad))
            }
            val vehicleLabel = assignedVehicle?.let { "${it.name} (${it.registrationNumber})" }
                ?: "RMC Municipal Unit"
            dao.insertLog(
                CollectionLogEntity(
                    locationId = point.id,
                    locationName = point.name,
                    status = "Collected",
                    wasteAmountKg = point.estimatedWasteKg,
                    timestamp = System.currentTimeMillis(),
                    assignedVehicle = vehicleLabel
                )
            )
        } else if (previousStatus == "Collected" && assignedVehicle != null) {
            // Reverted from Collected to Pending/Scheduled
            val newLoad = (assignedVehicle.currentLoadKg - point.estimatedWasteKg).coerceAtLeast(0)
            dao.updateVehicle(assignedVehicle.copy(currentLoadKg = newLoad))
            dao.insertLog(
                CollectionLogEntity(
                    locationId = point.id,
                    locationName = point.name,
                    status = newStatus,
                    wasteAmountKg = point.estimatedWasteKg,
                    timestamp = System.currentTimeMillis(),
                    assignedVehicle = "${assignedVehicle.name} (${assignedVehicle.registrationNumber})"
                )
            )
        }

        normalizeRouteOrdersAndRecalculate()
    }

    suspend fun runRouteOptimization(): OptimizationComparison {
        val allPoints = dao.getAllGarbagePointsOnce()
        val vehicles = dao.getAllVehiclesOnce()

        // Calculate available capacity from assigned vehicle (or total active fleet if none specifically assigned)
        val assignedVehicle = vehicles.firstOrNull { it.isAssignedToRoute && it.isActive }
        val availableCapacityKg = if (assignedVehicle != null) {
            (assignedVehicle.payloadCapacityKg - assignedVehicle.currentLoadKg).coerceAtLeast(600)
        } else {
            val activeSum = vehicles.filter { it.isActive }
                .sumOf { (it.payloadCapacityKg - it.currentLoadKg).coerceAtLeast(0) }
            if (activeSum > 0) activeSum else 4500
        }

        // Baseline (Before) stops in their unoptimized/current order
        val uncollected = allPoints.filter { it.status != "Collected" }
        val beforeStops = uncollected.sortedWith(
            compareBy<GarbagePointEntity> { if (it.routeOrder > 0) it.routeOrder else Int.MAX_VALUE }
                .thenBy { it.id }
        )

        val beforeRoute = RouteEngine.fetchRoadRoute(
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            stops = beforeStops
        )

        // Compute optimized sequence
        val (optimizedStops, skippedStops) = RouteEngine.computeOptimizedSequence(
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            candidates = uncollected,
            availableVehicleCapacityKg = availableCapacityKg
        )

        val afterRoute = RouteEngine.fetchRoadRoute(
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            stops = optimizedStops
        )

        // Update routeOrder in DB
        val updatedEntities = allPoints.map { pt ->
            if (pt.status == "Collected") {
                pt.copy(routeOrder = 0)
            } else {
                val idx = optimizedStops.indexOfFirst { it.id == pt.id }
                if (idx >= 0) {
                    val newStatus = if (pt.status == "Pending") "Scheduled" else pt.status
                    pt.copy(routeOrder = idx + 1, status = newStatus)
                } else {
                    pt.copy(routeOrder = 0)
                }
            }
        }
        dao.updateGarbagePoints(updatedEntities)

        val existingMeta = dao.getRouteMetadataOnce()
        // Keep original unoptimized baseline if before distance was higher or not yet optimized
        val baselineDist = if (existingMeta != null && !existingMeta.isOptimized && existingMeta.beforeDistanceKm > 0) {
            existingMeta.beforeDistanceKm
        } else {
            beforeRoute.distanceKm
        }
        val baselineDur = if (existingMeta != null && !existingMeta.isOptimized && existingMeta.beforeDurationMin > 0) {
            existingMeta.beforeDurationMin
        } else {
            beforeRoute.durationMinutes
        }
        val baselineCount = if (existingMeta != null && !existingMeta.isOptimized && existingMeta.beforeStopCount > 0) {
            existingMeta.beforeStopCount
        } else {
            beforeStops.size
        }

        val newMeta = RouteMetadataEntity(
            id = 1,
            isOptimized = true,
            beforeDistanceKm = baselineDist,
            beforeDurationMin = baselineDur,
            beforeStopCount = baselineCount,
            currentDistanceKm = afterRoute.distanceKm,
            currentDurationMin = afterRoute.durationMinutes,
            currentStopCount = optimizedStops.size,
            routingSource = afterRoute.sourceLabel,
            encodedPolylineJson = RouteEngine.encodeGeometryToJson(afterRoute.geometryPoints),
            startName = SampleRajkotData.DEPOT_NAME,
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            lastUpdated = System.currentTimeMillis()
        )
        dao.saveRouteMetadata(newMeta)

        return OptimizationComparison(
            beforeDistanceKm = baselineDist,
            beforeDurationMin = baselineDur,
            beforeStopCount = baselineCount,
            afterDistanceKm = afterRoute.distanceKm,
            afterDurationMin = afterRoute.durationMinutes,
            afterStopCount = optimizedStops.size,
            orderedStops = updatedEntities.filter { it.routeOrder > 0 }.sortedBy { it.routeOrder },
            skippedForCapacity = skippedStops,
            routeResult = afterRoute
        )
    }

    /**
     * Moves a stop up (-1) or down (+1) in the route sequence and recalculates real road route.
     */
    suspend fun moveRouteStop(pointId: Int, direction: Int): RoadRouteResult? {
        val allPoints = dao.getAllGarbagePointsOnce()
        val orderedRoute = allPoints
            .filter { it.status != "Collected" && it.routeOrder > 0 }
            .sortedBy { it.routeOrder }
            .toMutableList()

        val currentIndex = orderedRoute.indexOfFirst { it.id == pointId }
        if (currentIndex == -1) return null

        val targetIndex = currentIndex + direction
        if (targetIndex !in orderedRoute.indices) return null

        val temp = orderedRoute[currentIndex]
        orderedRoute[currentIndex] = orderedRoute[targetIndex]
        orderedRoute[targetIndex] = temp

        val updatedRoutePoints = orderedRoute.mapIndexed { index, pt ->
            pt.copy(routeOrder = index + 1)
        }
        dao.updateGarbagePoints(updatedRoutePoints)

        return recalculateCurrentRouteMetrics(preserveBaseline = true)
    }

    private suspend fun normalizeRouteOrdersAndRecalculate() {
        val allPoints = dao.getAllGarbagePointsOnce()
        val activeOrdered = allPoints
            .filter { it.status != "Collected" }
            .sortedWith(
                compareBy<GarbagePointEntity> { if (it.routeOrder > 0) it.routeOrder else Int.MAX_VALUE }
                    .thenBy { it.id }
            )

        val reindexed = allPoints.map { pt ->
            if (pt.status == "Collected") {
                pt.copy(routeOrder = 0)
            } else {
                val idx = activeOrdered.indexOfFirst { it.id == pt.id }
                pt.copy(routeOrder = if (idx >= 0) idx + 1 else 0)
            }
        }
        dao.updateGarbagePoints(reindexed)
        recalculateCurrentRouteMetrics(preserveBaseline = true)
    }

    suspend fun recalculateCurrentRouteMetrics(preserveBaseline: Boolean): RoadRouteResult {
        val allPoints = dao.getAllGarbagePointsOnce()
        val activeStops = allPoints
            .filter { it.status != "Collected" && it.routeOrder > 0 }
            .sortedBy { it.routeOrder }

        val routeResult = RouteEngine.fetchRoadRoute(
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            stops = activeStops
        )

        val existing = dao.getRouteMetadataOnce()
        val meta = RouteMetadataEntity(
            id = 1,
            isOptimized = existing?.isOptimized ?: false,
            beforeDistanceKm = if (preserveBaseline && existing != null && existing.beforeDistanceKm > 0) {
                existing.beforeDistanceKm
            } else {
                routeResult.distanceKm
            },
            beforeDurationMin = if (preserveBaseline && existing != null && existing.beforeDurationMin > 0) {
                existing.beforeDurationMin
            } else {
                routeResult.durationMinutes
            },
            beforeStopCount = if (preserveBaseline && existing != null && existing.beforeStopCount > 0) {
                existing.beforeStopCount
            } else {
                activeStops.size
            },
            currentDistanceKm = routeResult.distanceKm,
            currentDurationMin = routeResult.durationMinutes,
            currentStopCount = activeStops.size,
            routingSource = routeResult.sourceLabel,
            encodedPolylineJson = RouteEngine.encodeGeometryToJson(routeResult.geometryPoints),
            startName = SampleRajkotData.DEPOT_NAME,
            startLat = SampleRajkotData.DEPOT_LAT,
            startLon = SampleRajkotData.DEPOT_LON,
            lastUpdated = System.currentTimeMillis()
        )
        dao.saveRouteMetadata(meta)
        return routeResult
    }

    // Fleet management operations
    suspend fun saveVehicle(vehicle: VehicleEntity) {
        if (vehicle.id == 0) {
            dao.insertVehicle(vehicle)
        } else {
            dao.updateVehicle(vehicle)
        }
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        dao.deleteVehicle(vehicle)
    }

    suspend fun assignVehicleToRoute(vehicleId: Int) {
        val all = dao.getAllVehiclesOnce()
        val updated = all.map { v ->
            if (v.id == vehicleId) {
                v.copy(isAssignedToRoute = true, isActive = true)
            } else {
                v.copy(isAssignedToRoute = false)
            }
        }
        dao.updateVehicles(updated)
    }
}
