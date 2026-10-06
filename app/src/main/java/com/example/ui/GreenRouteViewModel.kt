package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CollectionLogEntity
import com.example.data.GarbagePointEntity
import com.example.data.GreenRouteDatabase
import com.example.data.GreenRouteRepository
import com.example.data.RouteMetadataEntity
import com.example.data.SampleRajkotData
import com.example.data.VehicleEntity
import com.example.domain.LatLon
import com.example.domain.RouteEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    LIVE_MAP("live_map", "Live Map"),
    LOCATIONS("locations", "Locations"),
    OPTIMIZE("optimize", "Optimize"),
    FLEET("fleet", "Fleet"),
    ANALYTICS("analytics", "Analytics")
}

data class GreenRouteUiState(
    val isLoggedIn: Boolean = false,
    val loggedInEmail: String = "",
    val loginError: String? = null,
    val currentTab: AppTab = AppTab.DASHBOARD,
    val isInitializing: Boolean = true,
    val isRoutingLoading: Boolean = false,
    val statusBannerMessage: String? = null,
    val isWarningBanner: Boolean = false,
    val searchQuery: String = "",
    val selectedStatusFilter: String = "All", // All, Pending, Scheduled, Collected
    val selectedRiskFilter: String = "All",   // All, Critical, High, Medium, Low
    val sortByPriorityDesc: Boolean = true,
    val skippedCapacityNames: List<String> = emptyList()
)

class GreenRouteViewModel(private val repository: GreenRouteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GreenRouteUiState())
    val uiState: StateFlow<GreenRouteUiState> = _uiState.asStateFlow()

    val garbagePoints: StateFlow<List<GarbagePointEntity>> = repository.garbagePointsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles: StateFlow<List<VehicleEntity>> = repository.vehiclesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionLogs: StateFlow<List<CollectionLogEntity>> = repository.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routeMetadata: StateFlow<RouteMetadataEntity?> = repository.routeMetadataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val orderedRouteStops: StateFlow<List<GarbagePointEntity>> = combine(garbagePoints) { ptsArr ->
        val pts = ptsArr[0]
        pts.filter { it.status != "Collected" && it.routeOrder > 0 }
            .sortedBy { it.routeOrder }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decodedRouteGeometry: StateFlow<List<LatLon>> = combine(
        routeMetadata,
        orderedRouteStops
    ) { meta, stops ->
        val decoded = meta?.encodedPolylineJson?.let { RouteEngine.decodeGeometryFromJson(it) }
            ?: emptyList()
        if (decoded.size >= 2) {
            decoded
        } else {
            // Fallback coordinates list from depot through ordered stops
            val list = mutableListOf(LatLon(SampleRajkotData.DEPOT_LAT, SampleRajkotData.DEPOT_LON))
            stops.forEach { list.add(LatLon(it.latitude, it.longitude)) }
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                repository.ensureSeededAndInitialized()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Database initialized with local fallback (${e.localizedMessage ?: "OK"})",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isInitializing = false)
            }
        }
    }

    fun loginWithCredentials(emailInput: String, passwordInput: String) {
        val email = emailInput.trim()
        val password = passwordInput.trim()
        if (email.equals("admin@gprajkot.ac.in", ignoreCase = true) && password == "rajkot2026") {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                loggedInEmail = "admin@gprajkot.ac.in",
                loginError = null,
                currentTab = AppTab.DASHBOARD
            )
        } else {
            _uiState.value = _uiState.value.copy(
                loginError = "Invalid credentials. Use admin@gprajkot.ac.in / rajkot2026 or tap Demo Admin."
            )
        }
    }

    fun loginAsDemoAdmin() {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = true,
            loggedInEmail = "admin@gprajkot.ac.in",
            loginError = null,
            currentTab = AppTab.DASHBOARD
        )
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            loggedInEmail = "",
            loginError = null
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(statusBannerMessage = null)
    }

    // Location Filters
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateStatusFilter(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
    }

    fun updateRiskFilter(risk: String) {
        _uiState.value = _uiState.value.copy(selectedRiskFilter = risk)
    }

    fun togglePrioritySort() {
        _uiState.value = _uiState.value.copy(sortByPriorityDesc = !_uiState.value.sortByPriorityDesc)
    }

    // CRUD Operations for Garbage Points
    fun saveGarbagePoint(point: GarbagePointEntity) {
        if (_uiState.value.isRoutingLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true)
            try {
                repository.addOrUpdateGarbagePoint(point)
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = if (point.id == 0) "Added '${point.name}' & updated Rajkot route" else "Updated '${point.name}'",
                    isWarningBanner = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Error saving location: ${e.localizedMessage}",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    fun deleteGarbagePoint(point: GarbagePointEntity) {
        if (_uiState.value.isRoutingLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true)
            try {
                repository.deleteGarbagePoint(point)
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Deleted '${point.name}' & recalculated route",
                    isWarningBanner = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Delete failed: ${e.localizedMessage}",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    fun cycleOrSetPointStatus(point: GarbagePointEntity, targetStatus: String? = null) {
        if (_uiState.value.isRoutingLoading) return
        val nextStatus = targetStatus ?: when (point.status) {
            "Pending" -> "Scheduled"
            "Scheduled" -> "Collected"
            else -> "Pending"
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true)
            try {
                repository.updatePointStatus(point, nextStatus)
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "${point.name} marked as $nextStatus",
                    isWarningBanner = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Could not update status: ${e.localizedMessage}",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    // Route Optimization & Reordering
    fun optimizeRouteNow() {
        if (_uiState.value.isRoutingLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true, statusBannerMessage = null)
            try {
                val comparison = repository.runRouteOptimization()
                val skippedNames = comparison.skippedForCapacity.map { it.name }
                val warning = comparison.routeResult.warningMessage
                val msg = when {
                    warning != null -> warning
                    skippedNames.isNotEmpty() -> "Route optimized (${comparison.afterStopCount} stops). ${skippedNames.size} deferred due to vehicle capacity."
                    else -> "Route optimized via OSRM road network: ${comparison.afterDistanceKm} km (${comparison.afterDurationMin} min)"
                }
                _uiState.value = _uiState.value.copy(
                    skippedCapacityNames = skippedNames,
                    statusBannerMessage = msg,
                    isWarningBanner = warning != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Optimization error: ${e.localizedMessage}",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    fun moveStopInRoute(pointId: Int, moveUp: Boolean) {
        if (_uiState.value.isRoutingLoading) return
        val direction = if (moveUp) -1 else 1
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true)
            try {
                val res = repository.moveRouteStop(pointId, direction)
                if (res != null) {
                    val warn = res.warningMessage
                    _uiState.value = _uiState.value.copy(
                        statusBannerMessage = warn ?: "Stop order updated: ${res.distanceKm} km, ${res.durationMinutes} min",
                        isWarningBanner = warn != null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = "Failed to reorder stop: ${e.localizedMessage}",
                    isWarningBanner = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    fun refreshRoadRoute() {
        if (_uiState.value.isRoutingLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRoutingLoading = true)
            try {
                val res = repository.recalculateCurrentRouteMetrics(preserveBaseline = true)
                _uiState.value = _uiState.value.copy(
                    statusBannerMessage = res.warningMessage ?: "Road route synced (${res.distanceKm} km)",
                    isWarningBanner = res.warningMessage != null
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRoutingLoading = false)
            }
        }
    }

    // Fleet CRUD
    fun saveVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.saveVehicle(vehicle)
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = if (vehicle.id == 0) "Added vehicle ${vehicle.name}" else "Updated vehicle ${vehicle.name}",
                isWarningBanner = false
            )
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Removed vehicle ${vehicle.name}",
                isWarningBanner = false
            )
        }
    }

    fun assignVehicleToActiveRoute(vehicle: VehicleEntity) {
        viewModelScope.launch {
            repository.assignVehicleToRoute(vehicle.id)
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "Assigned ${vehicle.name} (${vehicle.payloadCapacityKg} kg cap) to active route",
                isWarningBanner = false
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = GreenRouteDatabase.getDatabase(context)
                    val repo = GreenRouteRepository(db.dao())
                    return GreenRouteViewModel(repo) as T
                }
            }
        }
    }
}
