package com.gymapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing each destination reachable via the bottom navigation bar.
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Workouts : BottomNavDestination(
        route = "workouts",
        label = "Workouts",
        icon = Icons.Filled.FitnessCenter,
    )

    data object History : BottomNavDestination(
        route = "history",
        label = "History",
        icon = Icons.Filled.History,
    )

    data object Settings : BottomNavDestination(
        route = "settings",
        label = "Settings",
        icon = Icons.Filled.Settings,
    )
}

val bottomNavDestinations: List<BottomNavDestination> = listOf(
    BottomNavDestination.Workouts,
    BottomNavDestination.History,
    BottomNavDestination.Settings,
)
