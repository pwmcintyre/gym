package com.gymapp.feature.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.SessionSummary
import com.gymapp.core.model.WorkoutSession
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatVolume
import com.gymapp.core.model.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onOpenWorkout: (sessionId: String) -> Unit,
    onOpenDetail: (sessionId: String) -> Unit,
    onOpenProgress: (exerciseName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = hiltViewModel(),
    aiViewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val progressSummaries by viewModel.progressSummaries.collectAsStateWithLifecycle()
    val sessionSummaries by viewModel.sessionSummaries.collectAsStateWithLifecycle()
    var showAiSheet by rememberSaveable { mutableStateOf(false) }

    // Configure AI in history mode (no session)
    LaunchedEffect(Unit) {
        aiViewModel.configure(sessionId = null, workoutMode = false)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Workouts") }) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FloatingActionButton(
                    onClick = { showAiSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant")
                }
                FloatingActionButton(
                    onClick = { viewModel.createWorkout(onOpenWorkout) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New workout")
                }
            }
        },
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    "No workouts yet. Tap + to start.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (progressSummaries.isNotEmpty()) {
                    item {
                        ProgressOverviewCard(
                            progressSummaries = progressSummaries,
                            onOpenProgress = onOpenProgress,
                        )
                    }
                }
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        summary = sessionSummaries[session.id],
                        onClick = { onOpenDetail(session.id) },
                        onCopyAsTemplate = {
                            viewModel.createFromTemplate(session.id, onOpenWorkout)
                        },
                    )
                }
            }
        }
    }

    if (showAiSheet) {
        AiAssistantSheet(
            onDismiss = { showAiSheet = false },
            viewModel = aiViewModel,
        )
    }
}

@Composable
private fun ProgressOverviewCard(
    progressSummaries: List<ExerciseProgressSummary>,
    onOpenProgress: (exerciseName: String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progress Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Recent movements ranked by the latest logged session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            progressSummaries.forEachIndexed { index, summary ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProgress(summary.exerciseName) },
                ) {
                    Text(
                        text = summary.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildProgressSummaryLine(summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = "Last logged ${formatDate(summary.lastPerformed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    summary: SessionSummary?,
    onClick: () -> Unit,
    onCopyAsTemplate: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.notes?.takeIf { it.isNotBlank() } ?: formatDate(session.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                )
                val subtitleParts = buildList {
                    if (session.notes?.isNotBlank() == true) add(formatDate(session.date))
                    if (summary != null && summary.exerciseCount > 0) {
                        add("${summary.exerciseCount} exercise${if (summary.exerciseCount != 1) "s" else ""}")
                        add("${summary.setCount} set${if (summary.setCount != 1) "s" else ""}")
                    }
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.padding(bottom = 16.dp))
                }
            }
            IconButton(
                onClick = onCopyAsTemplate,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = "Copy as new workout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun buildProgressSummaryLine(summary: ExerciseProgressSummary): String = buildString {
    append("${summary.sessionCount} session")
    if (summary.sessionCount != 1) append("s")
    append(" • Best ${summary.bestWeight?.let { formatWeight(it) } ?: "—"}")
    append(" • Volume ${formatVolume(summary.totalVolume)}")
}
