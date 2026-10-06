package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garbage_points")
data class GarbagePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val wasteType: String, // "Mixed Municipal", "Organic / Wet", "Recyclable Dry", "Commercial / Bulk"
    val estimatedWasteKg: Int,
    val binCapacityKg: Int,
    val fillPercentage: Int,
    val priorityScore: Int, // 1 to 100
    val riskLevel: String, // "Critical", "High", "Medium", "Low"
    val status: String, // "Pending", "Scheduled", "Collected"
    val notes: String,
    val routeOrder: Int = 0 // 0 = not in route, 1..N = stop order in current route
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val registrationNumber: String,
    val fuelType: String, // "CNG", "Electric EV", "Diesel"
    val payloadCapacityKg: Int,
    val currentLoadKg: Int,
    val driverName: String,
    val driverContact: String,
    val isActive: Boolean,
    val isAssignedToRoute: Boolean = false
)

@Entity(tableName = "collection_logs")
data class CollectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationId: Int,
    val locationName: String,
    val status: String,
    val wasteAmountKg: Int,
    val timestamp: Long,
    val assignedVehicle: String
)

@Entity(tableName = "route_metadata")
data class RouteMetadataEntity(
    @PrimaryKey val id: Int = 1,
    val isOptimized: Boolean = false,
    val beforeDistanceKm: Double = 0.0,
    val beforeDurationMin: Int = 0,
    val beforeStopCount: Int = 0,
    val currentDistanceKm: Double = 0.0,
    val currentDurationMin: Int = 0,
    val currentStopCount: Int = 0,
    val routingSource: String = "OSRM Road Network",
    val encodedPolylineJson: String = "", // JSON array of [lat, lon] pairs
    val startName: String = "RMC Depot (Gov. Polytechnic Rajkot)",
    val startLat: Double = 22.2736,
    val startLon: Double = 70.8222,
    val lastUpdated: Long = System.currentTimeMillis()
)
