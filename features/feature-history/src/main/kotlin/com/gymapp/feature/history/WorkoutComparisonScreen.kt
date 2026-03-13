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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val minWeight = sorted.minOf { it.bestWeight }
    val maxWeight = sorted.maxOf { it.bestWeight }
    val range = (maxWeight - minWeight).coerceAtLeast(2.5f)
    val firstDate = sorted.first().sessionDate
    val latestDate = sorted.last().sessionDate

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progress Trend",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = gainLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = gainColor,
                    )
                    Text(
                        text = "vs first session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                buildAnnotatedString {
                    append("Best ")
                    withStyle(SpanStyle(color = primaryColor)) {
                        append(formatWeight(best))
                    }
                    append(" • ")
                    withStyle(SpanStyle(color = primaryColor)) {
                        append(sorted.size.toString())
                    }
                    append(" session")
                    if (sorted.size != 1) append("s")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = androidx.compose.ui.Alignment.End,
                    modifier = Modifier
                        .width(40.dp)
                        .height(120.dp)
                        .padding(end = 4.dp),
                ) {
                    Text(
                        text = formatWeight(maxWeight, appendUnit = false),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatWeight(minWeight, appendUnit = false),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                ) {
                    val pad = 6.dp.toPx()
                    val chartH = size.height
                    val step = if (sorted.size > 1) (size.width - pad * 2) / (sorted.size - 1) else size.width / 2f

                    drawLine(
                        color = axisColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, chartH),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, chartH),
                        end = Offset(size.width, chartH),
                        strokeWidth = 1.dp.toPx(),
                    )

                    val pts = sorted.mapIndexed { i, p ->
                        val x = pad + i * step
                        val y = chartH - pad - ((p.bestWeight - minWeight) / range) * (chartH - pad * 2)
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    pts.forEach { drawCircle(lineColor, 3.5.dp.toPx(), it) }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp),
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.Start) {
                    Text(
                        text = formatWeight(first),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatDate(firstDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    if (latest != first) {
                        Text(
                            text = formatWeight(latest),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = formatDate(latestDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (sorted.size >= 3) {
                Text(
                    text = "Trend spans ${sorted.size} sessions from ${formatDate(firstDate)} to ${formatDate(latestDate)}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
