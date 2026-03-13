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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.SessionSummary
import com.gymapp.core.model.WorkoutSession
import com.gymapp.core.model.formatDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onOpenWorkout: (sessionId: String) -> Unit,
    onOpenDetail: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = hiltViewModel(),
    aiViewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val sessionSummaries by viewModel.sessionSummaries.collectAsStateWithLifecycle()
    val suggestedNames by viewModel.suggestedNames.collectAsStateWithLifecycle()
    var showAiSheet by rememberSaveable { mutableStateOf(false) }

    val groupedSessions = remember(sessions) {
        sessions.groupBy { truncateToDay(it.date) }
            .entries.sortedByDescending { it.key }
    }

    // Configure AI in history mode (no session)
    LaunchedEffect(Unit) {
        aiViewModel.configure(sessionId = null, workoutMode = false)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                groupedSessions.forEach { (dayMs, daySessions) ->
                    item(key = "header_$dayMs", contentType = "header") {
                        Text(
                            text = dayLabel(dayMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(daySessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            summary = sessionSummaries[session.id],
                            suggestedName = suggestedNames[session.id],
                            onClick = { onOpenDetail(session.id) },
                        )
                    }
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
private fun SessionCard(
    session: WorkoutSession,
    summary: SessionSummary?,
    suggestedName: String?,
    onClick: () -> Unit,
) {
    val status = remember(summary) { sessionStatus(summary) }
    val nameText: String?
    val nameColor: androidx.compose.ui.graphics.Color

    when {
        !session.notes.isNullOrBlank() -> {
            nameText = session.notes!!
            nameColor = MaterialTheme.colorScheme.onSurface
        }
        suggestedName != null -> {
            nameText = suggestedName
            nameColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            nameText = null
            nameColor = MaterialTheme.colorScheme.onSurface
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = status.containerColor(),
        ),
        border = BorderStroke(1.dp, status.borderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatusChip(status = status, onClick = onClick)
                }
                if (nameText != null) {
                    Text(
                        text = nameText,
                        style = MaterialTheme.typography.titleMedium,
                        color = nameColor,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (summary != null && summary.exerciseCount > 0) {
                    val summaryText = buildString {
                        append("${summary.exerciseCount} movement${if (summary.exerciseCount != 1) "s" else ""}")
                        append(" · ")
                        append("${summary.setCount} set${if (summary.setCount != 1) "s" else ""}")
                    }
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val annotated = buildAnnotatedString {
                        var i = 0
                        while (i < summaryText.length) {
                            if (summaryText[i].isDigit()) {
                                val start = i
                                while (i < summaryText.length && summaryText[i].isDigit()) i++
                                withStyle(SpanStyle(color = primaryColor)) { append(summaryText.substring(start, i)) }
                            } else {
                                append(summaryText[i])
                                i++
                            }
                        }
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = if (nameText != null) 4.dp else 16.dp,
                        ),
                    )
                } else {
                    Text(
                        text = "No movements yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (nameText != null) 4.dp else 12.dp),
                    )
                }
            }
        }
    }
}

private enum class SessionStatus(val label: String) {
    PLANNED("Planned"),
    LOGGED("Logged"),
}

private fun sessionStatus(summary: SessionSummary?): SessionStatus =
    if (summary != null && summary.setCount > 0) SessionStatus.LOGGED else SessionStatus.PLANNED

@Composable
private fun StatusChip(status: SessionStatus, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(status.label) },
        leadingIcon = {
            Icon(
                imageVector = when (status) {
                    SessionStatus.PLANNED -> Icons.Default.Schedule
                    SessionStatus.LOGGED -> Icons.Default.Check
                },
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = status.chipContainerColor(),
            labelColor = status.chipLabelColor(),
            leadingIconContentColor = status.chipLabelColor(),
        ),
    )
}

@Composable
private fun SessionStatus.containerColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.surfaceVariant
    SessionStatus.LOGGED -> MaterialTheme.colorScheme.surface
}

@Composable
private fun SessionStatus.borderColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.outlineVariant
    SessionStatus.LOGGED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
}

@Composable
private fun SessionStatus.chipContainerColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.secondaryContainer
    SessionStatus.LOGGED -> MaterialTheme.colorScheme.primaryContainer
}

@Composable
private fun SessionStatus.chipLabelColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.onSecondaryContainer
    SessionStatus.LOGGED -> MaterialTheme.colorScheme.onPrimaryContainer
}

private fun truncateToDay(epochMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun dayLabel(epochMs: Long): String {
    val todayStart = truncateToDay(System.currentTimeMillis())
    return when {
        epochMs >= todayStart -> "Today"
        epochMs >= todayStart - 86_400_000L -> "Yesterday"
        else -> formatDate(epochMs)
    }
}
