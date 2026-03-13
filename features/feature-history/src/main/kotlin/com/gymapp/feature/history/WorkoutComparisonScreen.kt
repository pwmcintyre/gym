package com.gymapp.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseWorkoutProgression
import com.gymapp.core.model.WorkoutComparisonSession
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatVolume
import com.gymapp.core.model.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutComparisonScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutComparisonViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val progressions by viewModel.exerciseProgressions.collectAsStateWithLifecycle()

    // Group progressions by exercise name for the table
    val byExercise = remember(progressions) {
        progressions.groupBy { it.exerciseName }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(viewModel.workoutName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (sessions.size < 2) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Not enough data yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Complete at least 2 sessions named \"${viewModel.workoutName}\" to see comparisons.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                item {
                    VolumeChartCard(sessions = sessions)
                }
                if (byExercise.isNotEmpty()) {
                    item {
                        Text(
                            "Exercise Progression",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                    items(byExercise.entries.toList(), key = { it.key }) { (name, pts) ->
                        ExerciseProgressionCard(exerciseName = name, points = pts)
                    }
                }
            }
        }
    }
}

// ── Volume bar chart ──────────────────────────────────────────────────────────

@Composable
private fun VolumeChartCard(sessions: List<WorkoutComparisonSession>) {
    val maxVolume = sessions.maxOfOrNull { it.totalVolume }?.takeIf { it > 0f } ?: 1f
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Volume per session",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${sessions.size} sessions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val display = sessions.takeLast(12) // show at most 12 bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val spacing = 10.dp.toPx()
                val count = display.size.coerceAtLeast(1)
                val availW = size.width - spacing * (count + 1)
                val barW = (availW / count).coerceAtLeast(8.dp.toPx())
                val chartH = size.height - 8.dp.toPx()

                drawLine(axisColor, Offset(0f, chartH), Offset(size.width, chartH), 1.dp.toPx())

                val points = mutableListOf<Offset>()
                display.forEachIndexed { i, session ->
                    val left = spacing + i * (barW + spacing)
                    val cx = left + barW / 2f
                    val h = if (session.totalVolume <= 0f) 4.dp.toPx()
                             else (session.totalVolume / maxVolume) * (chartH - 12.dp.toPx())
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, chartH - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                    val cy = chartH - (session.totalVolume / maxVolume) * (chartH - 12.dp.toPx())
                    points.add(Offset(cx, cy))
                }

                if (points.size >= 2) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    points.forEach { drawCircle(lineColor, 3.5.dp.toPx(), it) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                sessions.takeLast(12).forEach { s ->
                    Text(
                        formatDate(s.sessionDate, "M/d"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Best and latest volume stats
            val best = sessions.maxOf { it.totalVolume }
            val latest = sessions.last().totalVolume
            Text(
                "Best: ${formatVolume(best)}  |  Last: ${formatVolume(latest)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Per-exercise progression card ─────────────────────────────────────────────

@Composable
private fun ExerciseProgressionCard(
    exerciseName: String,
    points: List<ExerciseWorkoutProgression>,
) {
    val sorted = points.sortedBy { it.sessionDate }
    val best = sorted.maxOf { it.bestWeight }
    val first = sorted.first().bestWeight
    val latest = sorted.last().bestWeight
    val gain = latest - first
    val gainLabel = when {
        gain > 0f -> "+${formatWeight(gain)}"
        gain < 0f -> formatWeight(gain)
        else -> "="
    }
    val gainColor = when {
        gain > 0f -> MaterialTheme.colorScheme.primary
        gain < 0f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    gainLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = gainColor,
                )
            }
            Text(
                "Best: ${formatWeight(best)}  •  ${sorted.size} session${if (sorted.size != 1) "s" else ""}  •  ${formatWeight(first)} → ${formatWeight(latest)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
