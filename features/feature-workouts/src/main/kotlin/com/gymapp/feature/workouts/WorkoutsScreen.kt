package com.gymapp.feature.workouts

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutsScreen(
    onOpenWorkout: (sessionId: String) -> Unit,
    onOpenDetail: (sessionId: String) -> Unit,
    onOpenScan: () -> Unit,
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
    val todaysFocus = remember(groupedSessions, sessionSummaries, suggestedNames) {
        val todayKey = truncateToDay(System.currentTimeMillis())
        val todaySessions = groupedSessions.firstOrNull { it.key == todayKey }?.value.orEmpty()
        val prioritized = todaySessions.firstOrNull {
            sessionStatus(sessionSummaries[it.id]) != SessionStatus.COMPLETED
        } ?: todaySessions.firstOrNull()
        prioritized?.let { session ->
            FocusSession(
                session = session,
                name = session.notes?.takeIf { it.isNotBlank() } ?: suggestedNames[session.id],
                status = sessionStatus(sessionSummaries[session.id]),
            )
        }
    }

    // Configure AI in history mode (no session)
    LaunchedEffect(Unit) {
        aiViewModel.configure(sessionId = null, workoutMode = false)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAiSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant")
            }
        },
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                WorkoutsStartActions(
                    onNewWorkout = { viewModel.createWorkout(onOpenWorkout) },
                    onScanWorkout = onOpenScan,
                )
                Text(
                    "No workouts yet. Start with a fresh workout, ask for a suggestion, or scan a board.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
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
                todaysFocus?.let { focus ->
                    item(key = "today_focus", contentType = "focus") {
                        TodaysFocusCard(
                            focus = focus,
                            onOpen = {
                                if (focus.status == SessionStatus.COMPLETED) onOpenDetail(focus.session.id)
                                else onOpenWorkout(focus.session.id)
                            },
                        )
                    }
                }
                item(key = "hero_cta", contentType = "hero") {
                    WorkoutsStartActions(
                        onNewWorkout = { viewModel.createWorkout(onOpenWorkout) },
                        onScanWorkout = onOpenScan,
                    )
                }
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
                            onStartWorkout = { onOpenWorkout(session.id) },
                            onCopyAsTemplate = {
                                viewModel.createFromTemplate(session.id, onOpenWorkout)
                            },
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
private fun WorkoutsStartActions(
    onNewWorkout: () -> Unit,
    onScanWorkout: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StartActionButton(
            icon = Icons.Default.Add,
            contentDescription = "New workout",
            onClick = onNewWorkout,
            modifier = Modifier.weight(1f),
        )
        StartActionButton(
            icon = Icons.Default.CameraAlt,
            contentDescription = "Scan board",
            onClick = onScanWorkout,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StartActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(56.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = when (contentDescription) {
                    "New workout" -> "New"
                    "Scan board" -> "Scan"
                    else -> contentDescription
                },
            )
        }
    }
}

@Composable
private fun TodaysFocusCard(
    focus: FocusSession,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (focus.status) {
                SessionStatus.PLANNED -> MaterialTheme.colorScheme.primaryContainer
                SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                SessionStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = BorderStroke(
            1.dp,
            when (focus.status) {
                SessionStatus.PLANNED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                SessionStatus.COMPLETED -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Today's focus",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = focus.name ?: "Workout ready",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when (focus.status) {
                    SessionStatus.PLANNED -> "Planned for today"
                    SessionStatus.IN_PROGRESS -> "In progress today"
                    SessionStatus.COMPLETED -> "Completed today"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpen) {
                Icon(
                    imageVector = if (focus.status == SessionStatus.COMPLETED) {
                        Icons.Default.Check
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text(if (focus.status == SessionStatus.COMPLETED) "Open details" else "Start")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    session: WorkoutSession,
    summary: SessionSummary?,
    suggestedName: String?,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onCopyAsTemplate: () -> Unit,
) {
    val status = remember(summary) { sessionStatus(summary) }
    val progressState = remember(summary) { sessionProgressState(summary) }
    var showMenu by rememberSaveable(session.id) { mutableStateOf(false) }
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                ) {
                    if (nameText != null) {
                        Text(
                            text = nameText,
                            style = MaterialTheme.typography.titleMedium,
                            color = nameColor,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                    if (summary != null && summary.exerciseCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = if (nameText != null) 6.dp else 0.dp, end = 16.dp),
                        ) {
                            SummaryStat(
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                text = "${summary.exerciseCount} movement${if (summary.exerciseCount != 1) "s" else ""}",
                            )
                            SummaryStat(
                                icon = Icons.Default.FitnessCenter,
                                text = "${summary.setCount} set${if (summary.setCount != 1) "s" else ""}",
                            )
                        }
                        if (summary.targetSetCount > 0) {
                            val progress = (summary.setCount.toFloat() / summary.targetSetCount.toFloat())
                                .coerceIn(0f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text(
                                    text = "${summary.setCount} / ${summary.targetSetCount} sets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            SessionStateHint(
                                progressState = progressState,
                                modifier = Modifier.padding(top = 8.dp, end = 16.dp),
                            )
                        }
                        if (status != SessionStatus.COMPLETED) {
                            TextButton(
                                onClick = onStartWorkout,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text("Start")
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = if (nameText != null) 6.dp else 0.dp, end = 16.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "No movements yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = onStartWorkout,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Start")
                        }
                    }
                }
                Icon(
                    imageVector = when (status) {
                        SessionStatus.PLANNED -> Icons.Default.Schedule
                        SessionStatus.IN_PROGRESS -> Icons.Default.Timelapse
                        SessionStatus.COMPLETED -> Icons.Default.Check
                    },
                    contentDescription = status.label,
                    tint = when (status) {
                        SessionStatus.PLANNED -> MaterialTheme.colorScheme.outline
                        SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                        SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(20.dp),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                DropdownMenuItem(
                    text = { Text("Copy as new workout") },
                    onClick = {
                        showMenu = false
                        onCopyAsTemplate()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Open details") },
                    onClick = {
                        showMenu = false
                        onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(14.dp),
        )
        val primaryColor = MaterialTheme.colorScheme.primary
        val annotated = buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                if (text[i].isDigit()) {
                    val start = i
                    while (i < text.length && text[i].isDigit()) i++
                    withStyle(SpanStyle(color = primaryColor)) { append(text.substring(start, i)) }
                } else {
                    append(text[i])
                    i++
                }
            }
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionStateHint(
    progressState: SessionProgressState,
    modifier: Modifier = Modifier,
) {
    val (icon, text, tint) = when (progressState) {
        SessionProgressState.EMPTY -> Triple(
            Icons.AutoMirrored.Filled.DirectionsRun,
            "Add movements to build this workout",
            MaterialTheme.colorScheme.outline,
        )
        SessionProgressState.READY_TO_LOG -> Triple(
            Icons.Default.Schedule,
            "Ready to start logging",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SessionProgressState.IN_PROGRESS -> Triple(
            Icons.Default.Timelapse,
            "Workout in progress",
            MaterialTheme.colorScheme.tertiary,
        )
        SessionProgressState.FLEXIBLE_LOGGING -> Triple(
            Icons.Default.Timelapse,
            "Logged without set targets",
            MaterialTheme.colorScheme.primary,
        )
        SessionProgressState.TRACKED -> Triple(
            Icons.Default.Check,
            "Target-tracked session",
            MaterialTheme.colorScheme.primary,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

private enum class SessionStatus(val label: String) {
    PLANNED("Planned"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
}

private data class FocusSession(
    val session: WorkoutSession,
    val name: String?,
    val status: SessionStatus,
)

private enum class SessionProgressState {
    EMPTY,
    READY_TO_LOG,
    IN_PROGRESS,
    FLEXIBLE_LOGGING,
    TRACKED,
}

private fun sessionStatus(summary: SessionSummary?): SessionStatus = when {
    summary == null || summary.setCount == 0 -> SessionStatus.PLANNED
    summary.targetSetCount > 0 && summary.setCount < summary.targetSetCount -> SessionStatus.IN_PROGRESS
    else -> SessionStatus.COMPLETED
}

private fun sessionProgressState(summary: SessionSummary?): SessionProgressState = when {
    summary == null || summary.exerciseCount == 0 -> SessionProgressState.EMPTY
    summary.targetSetCount > 0 && summary.setCount < summary.targetSetCount -> SessionProgressState.IN_PROGRESS
    summary.targetSetCount > 0 -> SessionProgressState.TRACKED
    summary.setCount == 0 -> SessionProgressState.READY_TO_LOG
    else -> SessionProgressState.FLEXIBLE_LOGGING
}

@Composable
private fun SessionStatus.containerColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.surfaceVariant
    SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    SessionStatus.COMPLETED -> MaterialTheme.colorScheme.surface
}

@Composable
private fun SessionStatus.borderColor(): Color = when (this) {
    SessionStatus.PLANNED -> MaterialTheme.colorScheme.outlineVariant
    SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
    SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
    val daysAgo = ((todayStart - epochMs) / 86_400_000L).toInt()
    val todayYear = Calendar.getInstance().apply { timeInMillis = todayStart }.get(Calendar.YEAR)
    val labelYear = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.YEAR)
    return when {
        epochMs >= todayStart -> "Today"
        epochMs >= todayStart - 86_400_000L -> "Yesterday"
        daysAgo in 2..6 -> formatDate(epochMs, "EEEE")
        labelYear == todayYear -> formatDate(epochMs, "EEEE, MMM d")
        else -> formatDate(epochMs, "EEEE, MMM d, yyyy")
    }
}
