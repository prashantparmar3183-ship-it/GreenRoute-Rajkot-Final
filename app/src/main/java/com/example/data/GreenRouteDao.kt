package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GreenRouteDao {
    // Garbage Points
    @Query("SELECT * FROM garbage_points ORDER BY id ASC")
    fun getAllGarbagePointsFlow(): Flow<List<GarbagePointEntity>>

    @Query("SELECT * FROM garbage_points ORDER BY id ASC")
    suspend fun getAllGarbagePointsOnce(): List<GarbagePointEntity>

    @Query("SELECT COUNT(*) FROM garbage_points")
    suspend fun getGarbagePointCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarbagePoint(point: GarbagePointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGarbagePoints(points: List<GarbagePointEntity>)

    @Update
    suspend fun updateGarbagePoint(point: GarbagePointEntity)

    @Update
    suspend fun updateGarbagePoints(points: List<GarbagePointEntity>)

    @Delete
    suspend fun deleteGarbagePoint(point: GarbagePointEntity)

    // Vehicles
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun getAllVehiclesFlow(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    suspend fun getAllVehiclesOnce(): List<VehicleEntity>

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getVehicleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVehicles(vehicles: List<VehicleEntity>)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Update
    suspend fun updateVehicles(vehicles: List<VehicleEntity>)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    // Collection Logs
    @Query("SELECT * FROM collection_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<CollectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CollectionLogEntity)

    // Route Metadata
    @Query("SELECT * FROM route_metadata WHERE id = 1")
    fun getRouteMetadataFlow(): Flow<RouteMetadataEntity?>

    @Query("SELECT * FROM route_metadata WHERE id = 1")
    suspend fun getRouteMetadataOnce(): RouteMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRouteMetadata(metadata: RouteMetadataEntity)
}
