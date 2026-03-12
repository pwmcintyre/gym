package com.gymapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymapp.feature.history.HistoryScreen
import com.gymapp.feature.history.WorkoutDetailScreen
import com.gymapp.feature.scan.ScanScreen
import com.gymapp.feature.settings.SettingsScreen
import com.gymapp.feature.workouts.ActiveWorkoutScreen
import com.gymapp.feature.workouts.WorkoutsScreen
import com.gymapp.navigation.bottomNavDestinations
import com.gymapp.ui.theme.GymAppTheme

private object Routes {
    const val WORKOUTS = "workouts"
    const val ACTIVE_WORKOUT = "workout/{sessionId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/detail/{sessionId}"
    const val SCAN = "scan"
    const val SETTINGS = "settings"

    fun activeWorkout(sessionId: String) = "workout/$sessionId"
    fun historyDetail(sessionId: String) = "history/detail/$sessionId"
}

private val bottomNavRoutes = setOf(
    Routes.WORKOUTS, Routes.HISTORY, Routes.SCAN, Routes.SETTINGS,
)

@Composable
fun GymAppRoot() {
    GymAppTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (currentRoute in bottomNavRoutes) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavDestinations.forEach { destination ->
                            val selected = navBackStackEntry?.destination
                                ?.hierarchy
                                ?.any { it.route == destination.route } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.WORKOUTS,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.WORKOUTS) {
                    WorkoutsScreen(
                        onOpenWorkout = { navController.navigate(Routes.activeWorkout(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(
                    route = Routes.ACTIVE_WORKOUT,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                ) {
                    ActiveWorkoutScreen(
                        onBack = { navController.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onOpenDetail = { navController.navigate(Routes.historyDetail(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(
                    route = Routes.HISTORY_DETAIL,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                ) {
                    WorkoutDetailScreen(
                        onBack = { navController.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(Routes.SCAN) {
                    ScanScreen(
                        onWorkoutReady = { sessionId ->
                            navController.navigate(Routes.activeWorkout(sessionId)) {
                                popUpTo(Routes.SCAN) { inclusive = false }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
