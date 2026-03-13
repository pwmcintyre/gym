package com.gymapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing each destination reachable via the bottom navigation bar.
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
) {
    data object Workouts : BottomNavDestination(
        route = "workouts",
        label = "Workouts",
        unselectedIcon = Icons.Outlined.FitnessCenter,
        selectedIcon = Icons.Filled.FitnessCenter,
    )

    data object Movements : BottomNavDestination(
        route = "movements",
        label = "Movements",
        unselectedIcon = Icons.AutoMirrored.Outlined.DirectionsRun,
        selectedIcon = Icons.AutoMirrored.Filled.DirectionsRun,
    )

    data object Settings : BottomNavDestination(
        route = "settings",
        label = "Settings",
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
    )
}

val bottomNavDestinations: List<BottomNavDestination> = listOf(
    BottomNavDestination.Workouts,
    BottomNavDestination.Movements,
    BottomNavDestination.Settings,
)
