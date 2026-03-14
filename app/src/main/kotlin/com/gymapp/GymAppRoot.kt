package com.gymapp

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymapp.feature.history.ExerciseProgressScreen
import com.gymapp.feature.history.MovementsScreen
import com.gymapp.feature.history.WorkoutComparisonScreen
import com.gymapp.feature.history.WorkoutDetailScreen
import com.gymapp.feature.scan.ScanScreen
import com.gymapp.feature.settings.SettingsScreen
import com.gymapp.feature.workouts.ActiveWorkoutScreen
import com.gymapp.feature.workouts.WorkoutsScreen
import com.gymapp.navigation.bottomNavDestinations
import com.gymapp.ui.theme.GymAppTheme

private object Routes {
    const val WORKOUTS = "workouts"
    const val MOVEMENTS = "movements"
    const val ACTIVE_WORKOUT = "workout/{sessionId}"
    const val HISTORY_DETAIL = "history/detail/{sessionId}"
    const val HISTORY_PROGRESS = "history/progress/{exerciseName}"
    const val WORKOUT_COMPARISON = "history/comparison/{workoutName}"
    const val SCAN = "scan"
    const val SETTINGS = "settings"

    fun activeWorkout(sessionId: String) = "workout/$sessionId"
    fun historyDetail(sessionId: String) = "history/detail/$sessionId"
    fun historyProgress(exerciseName: String) = "history/progress/${Uri.encode(exerciseName)}"
    fun workoutComparison(workoutName: String) = "history/comparison/${Uri.encode(workoutName)}"
}

private val bottomNavRoutes = setOf(
    Routes.WORKOUTS, Routes.MOVEMENTS, Routes.SETTINGS,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymAppRoot() {
    GymAppTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var chatOpen by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val chatViewModel: ChatViewModel = hiltViewModel()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (!chatOpen) {
                    FloatingActionButton(
                        onClick = { chatOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Open coach",
                        )
                    }
                }
            },
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
                        onOpenDetail = { navController.navigate(Routes.historyDetail(it)) },
                        onOpenScan = { navController.navigate(Routes.SCAN) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(Routes.MOVEMENTS) {
                    MovementsScreen(
                        onOpenProgress = { navController.navigate(Routes.historyProgress(it)) },
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

                composable(
                    route = Routes.HISTORY_DETAIL,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                ) {
                    WorkoutDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.activeWorkout(it)) },
                        onOpenProgress = { navController.navigate(Routes.historyProgress(it)) },
                        onCompare = { navController.navigate(Routes.workoutComparison(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(
                    route = Routes.WORKOUT_COMPARISON,
                    arguments = listOf(navArgument("workoutName") { type = NavType.StringType }),
                ) {
                    WorkoutComparisonScreen(
                        onBack = { navController.popBackStack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(
                    route = Routes.HISTORY_PROGRESS,
                    arguments = listOf(navArgument("exerciseName") { type = NavType.StringType }),
                ) {
                    ExerciseProgressScreen(
                        onBack = { navController.popBackStack() },
                        onOpenWorkout = { navController.navigate(Routes.historyDetail(it)) },
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

        if (chatOpen) {
            ChatOverlay(
                sheetState = sheetState,
                messages = chatViewModel.messages,
                isLoading = chatViewModel.isLoading,
                onSend = { chatViewModel.sendMessage(it) },
                onDismiss = { chatOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatOverlay(
    sheetState: androidx.compose.material3.SheetState,
    messages: List<UiChatMessage>,
    isLoading: Boolean,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom whenever messages or loading state changes
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() || isLoading) {
            listState.animateScrollToItem(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            // Header row with title and close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Coach",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close chat",
                    )
                }
            }

            // Scrollable message list — reverseLayout so newest is at bottom
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Loading indicator at top of reversed list = visually at bottom
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                items(messages.reversed()) { message ->
                    MessageBubble(message)
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...") },
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        onSend(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: UiChatMessage) {
    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
