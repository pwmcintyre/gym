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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseWorkoutProgression
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutComparisonScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutComparisonViewModel = hiltViewModel(),
) {
    val progressions by viewModel.exerciseProgressions.collectAsStateWithLifecycle()

    val byExercise = remember(progressions) {
        progressions.groupBy { it.exerciseName }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text(viewModel.workoutName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (progressions.isEmpty() || byExercise.values.all { it.size < 2 }) {
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
                    "Complete at least 2 sessions named \"${viewModel.workoutName}\" to see progression.",
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
                items(byExercise.entries.toList(), key = { it.key }) { (name, pts) ->
                    ExerciseProgressionCard(exerciseName = name, points = pts)
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressionCard(
    exerciseName: String,
    points: List<ExerciseWorkoutProgression>,
) {
    val sorted = points.sortedBy { it.sessionDate }
    val first = sorted.first().bestWeight
    val latest = sorted.last().bestWeight
    val best = sorted.maxOf { it.bestWeight }
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
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary

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

            if (sorted.size >= 2) {
                val minW = sorted.minOf { it.bestWeight }
                val maxW = sorted.maxOf { it.bestWeight }
                val range = (maxW - minW).coerceAtLeast(2.5f) // avoid flat line for equal values

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(top = 8.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val pad = 6.dp.toPx()
                    val step = if (sorted.size > 1) (w - pad * 2) / (sorted.size - 1) else w

                    val pts = sorted.mapIndexed { i, p ->
                        val x = pad + i * step
                        val y = h - pad - ((p.bestWeight - minW) / range) * (h - pad * 2)
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    pts.forEach { drawCircle(dotColor, 3.5.dp.toPx(), it) }
                }

                // Date labels under the chart
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    sorted.forEach { p ->
                        Text(
                            formatDate(p.sessionDate, "M/d"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
