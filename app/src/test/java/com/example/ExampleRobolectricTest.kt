package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GarbagePointEntity
import com.example.data.GreenRouteDatabase
import com.example.data.GreenRouteRepository
import com.example.data.SampleRajkotData
import com.example.data.VehicleEntity
import com.example.domain.RouteEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: GreenRouteDatabase
    private lateinit var repository: GreenRouteRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GreenRouteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GreenRouteRepository(db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GreenRoute", appName)
    }

    @Test
    fun `seed database and verify 10 Rajkot locations and 3 vehicles`() = runBlocking {
        repository.ensureSeededAndInitialized()

        val points = repository.garbagePointsFlow.first()
        val vehicles = repository.vehiclesFlow.first()
        val logs = repository.logsFlow.first()
        val meta = repository.routeMetadataFlow.first()

        assertEquals(10, points.size)
        assertEquals(3, vehicles.size)
        assertEquals(2, logs.size)
        assertNotNull(meta)

        // Ensure seeding again does NOT duplicate or reset data
        repository.ensureSeededAndInitialized()
        assertEquals(10, repository.garbagePointsFlow.first().size)
    }

    @Test
    fun `add edit delete location and update collection status`() = runBlocking {
        repository.ensureSeededAndInitialized()

        val newPoint = GarbagePointEntity(
            name = "Trikon Baug Central Bin",
            address = "Trikon Baug Chowk, Rajkot",
            latitude = 22.2970,
            longitude = 70.8020,
            wasteType = "Mixed Municipal",
            estimatedWasteKg = 300,
            binCapacityKg = 450,
            fillPercentage = 67,
            priorityScore = 85,
            riskLevel = "High",
            status = "Pending",
            notes = "Central bus stop bin"
        )
        repository.addOrUpdateGarbagePoint(newPoint)

        val afterAdd = repository.garbagePointsFlow.first()
        assertEquals(11, afterAdd.size)
        val added = afterAdd.first { it.name == "Trikon Baug Central Bin" }

        // Edit location
        repository.addOrUpdateGarbagePoint(added.copy(estimatedWasteKg = 400, fillPercentage = 89))
        val updated = repository.garbagePointsFlow.first().first { it.id == added.id }
        assertEquals(400, updated.estimatedWasteKg)

        // Mark status as Collected -> creates log and sets routeOrder to 0
        val logsBefore = repository.logsFlow.first().size
        repository.updatePointStatus(updated, "Collected")
        val collectedPt = repository.garbagePointsFlow.first().first { it.id == added.id }
        assertEquals("Collected", collectedPt.status)
        assertEquals(0, collectedPt.routeOrder)
        assertEquals(logsBefore + 1, repository.logsFlow.first().size)

        // Delete location
        repository.deleteGarbagePoint(collectedPt)
        assertEquals(10, repository.garbagePointsFlow.first().size)
    }

    @Test
    fun `route optimization reduces distance and manual stop reordering updates order`() = runBlocking {
        repository.ensureSeededAndInitialized()

        val comparison = repository.runRouteOptimization()
        assertTrue(comparison.beforeDistanceKm > 0.0)
        assertTrue(comparison.afterDistanceKm > 0.0)
        // Optimized route across East/West/North/South Rajkot should be shorter than deliberately zig-zag initial sequence
        assertTrue(comparison.afterDistanceKm <= comparison.beforeDistanceKm)
        assertEquals(8, comparison.afterStopCount)

        val orderedBeforeMove = repository.garbagePointsFlow.first()
            .filter { it.routeOrder > 0 }
            .sortedBy { it.routeOrder }
        val firstStopId = orderedBeforeMove[0].id
        val secondStopId = orderedBeforeMove[1].id

        // Move first stop down
        repository.moveRouteStop(firstStopId, +1)
        val orderedAfterMove = repository.garbagePointsFlow.first()
            .filter { it.routeOrder > 0 }
            .sortedBy { it.routeOrder }

        assertEquals(secondStopId, orderedAfterMove[0].id)
        assertEquals(firstStopId, orderedAfterMove[1].id)
    }

    @Test
    fun `vehicle CRUD and route assignment works`() = runBlocking {
        repository.ensureSeededAndInitialized()

        val newVehicle = VehicleEntity(
            name = "RMC MiniTipper-04",
            registrationNumber = "GJ-03-GA-7788",
            fuelType = "CNG",
            payloadCapacityKg = 3200,
            currentLoadKg = 0,
            driverName = "ureshbhai Solanki",
            driverContact = "+91 98765 43210",
            isActive = true
        )
        repository.saveVehicle(newVehicle)
        val allVehicles = repository.vehiclesFlow.first()
        assertEquals(4, allVehicles.size)

        val saved = allVehicles.first { it.name == "RMC MiniTipper-04" }
        repository.assignVehicleToRoute(saved.id)

        val afterAssign = repository.vehiclesFlow.first()
        assertTrue(afterAssign.first { it.id == saved.id }.isAssignedToRoute)
        assertEquals(1, afterAssign.count { it.isAssignedToRoute })

        repository.deleteVehicle(saved)
        assertEquals(3, repository.vehiclesFlow.first().size)
    }
}
