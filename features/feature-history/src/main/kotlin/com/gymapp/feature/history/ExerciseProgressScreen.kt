package com.gymapp.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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
import com.gymapp.core.model.ExercisePr
import com.gymapp.core.model.ExerciseSessionProgress
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatVolume
import com.gymapp.core.model.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    onBack: () -> Unit,
    onOpenWorkout: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseProgressViewModel = hiltViewModel(),
) {
    val sessionProgress by viewModel.sessionProgress.collectAsStateWithLifecycle()
    val prs by viewModel.prs.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(viewModel.exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (sessionProgress.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = "No logged sets yet for this exercise.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
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
                item {
                    ExerciseProgressSummaryCard(
                        exerciseName = viewModel.exerciseName,
                        sessionProgress = sessionProgress,
                    )
                }
                if (prs.isNotEmpty()) {
                    item {
                        ExerciseProgressPrCard(prs = prs)
                    }
                }
                if (sessionProgress.size > 1) {
                    item {
                        ExerciseProgressChartCard(
                            sessionProgress = sessionProgress.take(8).asReversed(),
                        )
                    }
                }
                items(sessionProgress, key = { it.sessionId }) { progress ->
                    ExerciseProgressSessionCard(
                        progress = progress,
                        onClick = { onOpenWorkout(progress.sessionId) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Personal Records card
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseProgressPrCard(prs: List<ExercisePr>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "All-time best weight per rep count.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Show up to 8 PRs in two columns
            val rows = prs.take(8).chunked(2)
            rows.forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    pair.forEach { pr ->
                        PrChip(pr = pr, modifier = Modifier.weight(1f))
                    }
                    // Fill second slot if the row only has one item
                    if (pair.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrChip(pr: ExercisePr, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Text(
                text = formatWeight(pr.weight),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${pr.reps} rep${if (pr.reps != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Combined volume-bar + weight-trend chart
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseProgressChartCard(sessionProgress: List<ExerciseSessionProgress>) {
    val maxVolume = sessionProgress.maxOfOrNull { it.totalVolume }?.takeIf { it > 0f } ?: 1f
    val maxWeight = sessionProgress.mapNotNull { it.bestWeight }.maxOrNull()?.takeIf { it > 0f } ?: 1f

    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Volume & Weight Trend",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Bars = volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                )
                Text(
                    text = "Line = best weight",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp),
            ) {
                val spacing = 12.dp.toPx()
                val barCount = sessionProgress.size.coerceAtLeast(1)
                val availableWidth = size.width - (spacing * (barCount + 1))
                val barWidth = (availableWidth / barCount).coerceAtLeast(8.dp.toPx())
                val chartHeight = size.height - 8.dp.toPx()

                // Baseline axis
                drawLine(
                    color = axisColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                )

                // Volume bars (subdued)
                sessionProgress.forEachIndexed { index, progress ->
                    val left = spacing + index * (barWidth + spacing)
                    val normalizedHeight = if (progress.totalVolume <= 0f) {
                        4.dp.toPx()
                    } else {
                        (progress.totalVolume / maxVolume) * (chartHeight - 12.dp.toPx())
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, chartHeight - normalizedHeight),
                        size = Size(barWidth, normalizedHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }

                // Weight trend line
                val weightPoints = sessionProgress.mapIndexedNotNull { index, progress ->
                    val w = progress.bestWeight ?: return@mapIndexedNotNull null
                    if (w <= 0f) return@mapIndexedNotNull null
                    val cx = spacing + index * (barWidth + spacing) + barWidth / 2f
                    val cy = chartHeight - (w / maxWeight) * (chartHeight - 12.dp.toPx())
                    Offset(cx, cy)
                }

                if (weightPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(weightPoints.first().x, weightPoints.first().y)
                        weightPoints.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Dots on the weight line
                weightPoints.forEach { pt ->
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                sessionProgress.forEach { progress ->
                    Text(
                        text = formatDate(progress.sessionDate, "M/d"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Summary snapshot card
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseProgressSummaryCard(
    exerciseName: String,
    sessionProgress: List<ExerciseSessionProgress>,
) {
    val totalVolume = sessionProgress.sumOf { it.totalVolume.toDouble() }.toFloat()
    val bestWeight = sessionProgress.mapNotNull { it.bestWeight }.maxOrNull()?.takeIf { it > 0f }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Progress Snapshot",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${sessionProgress.size} session${if (sessionProgress.size != 1) "s" else ""}" +
                    " • Best ${bestWeight?.let { formatWeight(it) } ?: "—"}" +
                    " • Volume ${formatVolume(totalVolume)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Per-session history card
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseProgressSessionCard(
    progress: ExerciseSessionProgress,
    onClick: () -> Unit,
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatDate(progress.sessionDate),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${progress.setCount} set${if (progress.setCount != 1) "s" else ""}" +
                    " • Best ${progress.bestWeight?.let { formatWeight(it) } ?: "—"}" +
                    " • Volume ${formatVolume(progress.totalVolume)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Open workout details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
