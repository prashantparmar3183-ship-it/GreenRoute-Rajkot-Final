package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.GreenRouteViewModel
import com.example.ui.components.CompactBanner
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FleetScreen
import com.example.ui.screens.LiveMapScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OptimizeScreen
import com.example.ui.theme.CivicGreenPrimary
import com.example.ui.theme.GreenRouteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenRouteTheme {
                GreenRouteApp()
            }
        }
    }
}

@Composable
fun GreenRouteApp(
    viewModel: GreenRouteViewModel = viewModel(
        factory = GreenRouteViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val garbagePoints by viewModel.garbagePoints.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val logs by viewModel.collectionLogs.collectAsStateWithLifecycle()
    val routeMetadata by viewModel.routeMetadata.collectAsStateWithLifecycle()
    val orderedRouteStops by viewModel.orderedRouteStops.collectAsStateWithLifecycle()
    val decodedRouteGeometry by viewModel.decodedRouteGeometry.collectAsStateWithLifecycle()

    if (!uiState.isLoggedIn) {
        LoginScreen(
            loginError = uiState.loginError,
            onLoginSubmit = viewModel::loginWithCredentials,
            onDemoQuickLogin = viewModel::loginAsDemoAdmin
        )
        return
    }

    // BackHandler returns to Dashboard from secondary tabs
    BackHandler(enabled = uiState.currentTab != AppTab.DASHBOARD) {
        viewModel.selectTab(AppTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CompactCivicTopBar(
                currentTab = uiState.currentTab,
                isRoutingLoading = uiState.isRoutingLoading,
                onLogout = viewModel::logout
            )
        },
        bottomBar = {
            CompactBottomNavigationBar(
                currentTab = uiState.currentTab,
                onSelectTab = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Optional dismissible status / routing feedback banner
            uiState.statusBannerMessage?.let { msg ->
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    CompactBanner(
                        message = msg,
                        isWarning = uiState.isWarningBanner,
                        onDismiss = viewModel::dismissBanner
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentTab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        garbagePoints = garbagePoints,
                        vehicles = vehicles,
                        logs = logs,
                        routeMetadata = routeMetadata,
                        onNavigateTab = viewModel::selectTab,
                        onQuickMarkCollected = { pt -> viewModel.cycleOrSetPointStatus(pt, "Collected") },
                        onOptimizeNow = viewModel::optimizeRouteNow
                    )

                    AppTab.LIVE_MAP -> LiveMapScreen(
                        garbagePoints = garbagePoints,
                        routeGeometry = decodedRouteGeometry,
                        routeMetadata = routeMetadata,
                        isRoutingLoading = uiState.isRoutingLoading,
                        onOptimizeNow = viewModel::optimizeRouteNow,
                        onRefreshRoadRoute = viewModel::refreshRoadRoute,
                        onUpdatePointStatus = viewModel::cycleOrSetPointStatus
                    )

                    AppTab.LOCATIONS -> LocationsScreen(
                        garbagePoints = garbagePoints,
                        searchQuery = uiState.searchQuery,
                        selectedStatusFilter = uiState.selectedStatusFilter,
                        selectedRiskFilter = uiState.selectedRiskFilter,
                        sortByPriorityDesc = uiState.sortByPriorityDesc,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onStatusFilterChange = viewModel::updateStatusFilter,
                        onRiskFilterChange = viewModel::updateRiskFilter,
                        onTogglePrioritySort = viewModel::togglePrioritySort,
                        onSavePoint = viewModel::saveGarbagePoint,
                        onDeletePoint = viewModel::deleteGarbagePoint,
                        onUpdateStatus = viewModel::cycleOrSetPointStatus
                    )

                    AppTab.OPTIMIZE -> OptimizeScreen(
                        orderedStops = orderedRouteStops,
                        routeMetadata = routeMetadata,
                        vehicles = vehicles,
                        skippedCapacityNames = uiState.skippedCapacityNames,
                        isRoutingLoading = uiState.isRoutingLoading,
                        onRunOptimization = viewModel::optimizeRouteNow,
                        onMoveStop = viewModel::moveStopInRoute,
                        onMarkStopCollected = { pt -> viewModel.cycleOrSetPointStatus(pt, "Collected") },
                        onNavigateToMap = { viewModel.selectTab(AppTab.LIVE_MAP) }
                    )

                    AppTab.FLEET -> FleetScreen(
                        vehicles = vehicles,
                        onSaveVehicle = viewModel::saveVehicle,
                        onDeleteVehicle = viewModel::deleteVehicle,
                        onAssignToRoute = viewModel::assignVehicleToActiveRoute
                    )

                    AppTab.ANALYTICS -> AnalyticsScreen(
                        garbagePoints = garbagePoints,
                        vehicles = vehicles,
                        logs = logs,
                        routeMetadata = routeMetadata
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactCivicTopBar(
    currentTab: AppTab,
    isRoutingLoading: Boolean,
    onLogout: () -> Unit
) {
    Surface(
        color = CivicGreenPrimary,
        contentColor = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = "GreenRoute",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GREENROUTE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentTab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Gov. Polytechnic Rajkot • Municipal Route Optimizer",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isRoutingLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactBottomNavigationBar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        AppTab.entries.forEach { tab ->
            val icon: ImageVector = when (tab) {
                AppTab.DASHBOARD -> Icons.Default.Dashboard
                AppTab.LIVE_MAP -> Icons.Default.Map
                AppTab.LOCATIONS -> Icons.Default.LocationOn
                AppTab.OPTIMIZE -> Icons.Default.AltRoute
                AppTab.FLEET -> Icons.Default.LocalShipping
                AppTab.ANALYTICS -> Icons.Default.BarChart
            }
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(19.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_${tab.route}")
            )
        }
    }
}
