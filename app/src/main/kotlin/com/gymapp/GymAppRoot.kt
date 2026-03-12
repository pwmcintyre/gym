package com.gymapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.gymapp.feature.settings.SettingsScreen
import com.gymapp.feature.workouts.ActiveWorkoutScreen
import com.gymapp.feature.workouts.WorkoutsScreen
import com.gymapp.navigation.BottomNavDestination
import com.gymapp.navigation.bottomNavDestinations
import com.gymapp.ui.theme.GymAppTheme

private object Routes {
    const val WORKOUTS = "workouts"
    const val ACTIVE_WORKOUT = "workout/{sessionId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/detail/{sessionId}"
    const val SETTINGS = "settings"

    fun activeWorkout(sessionId: String) = "workout/$sessionId"
    fun historyDetail(sessionId: String) = "history/detail/$sessionId"
}

@Composable
fun GymAppRoot() {
    GymAppTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val showBottomBar = currentDestination?.route in listOf(
            Routes.WORKOUTS, Routes.HISTORY, Routes.SETTINGS,
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavDestinations.forEach { destination ->
                            val selected = currentDestination
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
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
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
                        onOpenWorkout = { sessionId ->
                            navController.navigate(Routes.activeWorkout(sessionId))
                        },
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
                        onOpenDetail = { sessionId ->
                            navController.navigate(Routes.historyDetail(sessionId))
                        },
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

                composable(Routes.SETTINGS) {
                    SettingsScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
