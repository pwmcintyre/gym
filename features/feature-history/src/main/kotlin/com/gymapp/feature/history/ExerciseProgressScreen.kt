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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExercisePr
import com.gymapp.core.model.ExerciseSessionProgress
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight
import java.util.Calendar

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
    val groupedSessionProgress = remember(sessionProgress) {
        sessionProgress.groupBy { truncateToDay(it.sessionDate) }
            .entries
            .sortedByDescending { it.key }
    }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text(viewModel.exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename movement")
                    }
                },
            )
        },
    ) { innerPadding ->

    if (showRenameDialog) {
        RenameMovementDialog(
            currentName = viewModel.exerciseName,
            onConfirm = { newName ->
                viewModel.renameGlobally(newName) {
                    showRenameDialog = false
                    onBack()
                }
            },
            onDismiss = { showRenameDialog = false },
        )
    }
        if (sessionProgress.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    text = "No logged sets yet for this movement.",
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
                groupedSessionProgress.forEach { (dayMs, daySessions) ->
                    item(key = "header_$dayMs", contentType = "header") {
                        Text(
                            text = dayLabel(dayMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(daySessions, key = { it.sessionId }) { progress ->
                        ExerciseProgressSessionCard(
                            progress = progress,
                            onClick = { onOpenWorkout(progress.sessionId) },
                        )
                    }
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
    val topWeight = prs.maxWithOrNull(compareBy<ExercisePr> { it.weight }.thenBy { it.reps })
    val topReps = prs.maxWithOrNull(compareBy<ExercisePr> { it.reps }.thenBy { it.weight })
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                topWeight?.let { pr ->
                    PrSummaryMetric(
                        label = "Top weight",
                        value = formatWeight(pr.weight),
                        supporting = "${pr.reps} rep${if (pr.reps != 1) "s" else ""}",
                        modifier = Modifier.weight(1f),
                    )
                } ?: Box(modifier = Modifier.weight(1f))
                topReps?.let { pr ->
                    PrSummaryMetric(
                        label = "Top reps",
                        value = "${pr.reps}",
                        supporting = formatWeight(pr.weight),
                        modifier = Modifier.weight(1f),
                    )
                } ?: Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrSummaryMetric(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Combined volume-bar + weight-trend chart
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseProgressChartCard(sessionProgress: List<ExerciseSessionProgress>) {
    val weights = sessionProgress.mapNotNull { it.bestWeight }.filter { it > 0f }
    val minWeight = weights.minOrNull() ?: 0f
    val maxWeight = weights.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val weightRange = (maxWeight - minWeight).coerceAtLeast(2.5f)
    val reps = sessionProgress.mapNotNull { it.bestReps }.filter { it > 0 }
    val minReps = reps.minOrNull() ?: 0
    val maxReps = reps.maxOrNull()?.coerceAtLeast(1) ?: 1
    val repsRange = (maxReps - minReps).coerceAtLeast(1)

    val lineColor = MaterialTheme.colorScheme.primary
    val repsLineColor = Color(0xFF9C27B0)
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    val firstWeight = sessionProgress.firstNotNullOfOrNull { it.bestWeight }
    val lastWeight = sessionProgress.lastOrNull { it.bestWeight != null }?.bestWeight
    val firstReps = sessionProgress.firstNotNullOfOrNull { it.bestReps }
    val lastReps = sessionProgress.lastOrNull { it.bestReps != null }?.bestReps

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
                text = "Weight + Reps Trend",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-axis labels
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .width(40.dp)
                        .height(120.dp)
                        .padding(end = 4.dp),
                ) {
                    Text(
                        text = formatWeight(maxWeight, appendUnit = false),
                        style = MaterialTheme.typography.labelSmall,
                        color = lineColor,
                    )
                    Text(
                        text = formatWeight(minWeight, appendUnit = false),
                        style = MaterialTheme.typography.labelSmall,
                        color = lineColor,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                ) {
                    val pad = 6.dp.toPx()
                    val chartH = size.height
                    val count = sessionProgress.size.coerceAtLeast(1)
                    val step = if (count > 1) (size.width - pad * 2) / (count - 1) else size.width / 2f

                    // Y-axis line
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, chartH),
                        strokeWidth = 1.dp.toPx(),
                    )
                    // Baseline
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, chartH),
                        end = Offset(size.width, chartH),
                        strokeWidth = 1.dp.toPx(),
                    )

                    val pts = sessionProgress.mapIndexedNotNull { i, p ->
                        val w = p.bestWeight ?: return@mapIndexedNotNull null
                        if (w <= 0f) return@mapIndexedNotNull null
                        val x = pad + i * step
                        val y = chartH - pad - ((w - minWeight) / weightRange) * (chartH - pad * 2)
                        Offset(x, y)
                    }
                    val repPts = sessionProgress.mapIndexedNotNull { i, p ->
                        val r = p.bestReps ?: return@mapIndexedNotNull null
                        if (r <= 0) return@mapIndexedNotNull null
                        val x = pad + i * step
                        val y = chartH - pad - ((r - minReps).toFloat() / repsRange.toFloat()) * (chartH - pad * 2)
                        Offset(x, y)
                    }

                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            pts.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    if (repPts.size >= 2) {
                        val path = Path().apply {
                            moveTo(repPts.first().x, repPts.first().y)
                            repPts.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, repsLineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    pts.forEach { drawCircle(lineColor, 3.5.dp.toPx(), it) }
                    repPts.forEach { drawCircle(repsLineColor, 3.5.dp.toPx(), it) }
                }

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .width(32.dp)
                        .height(120.dp)
                        .padding(start = 4.dp),
                ) {
                    Text(
                        text = maxReps.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = repsLineColor,
                    )
                    Text(
                        text = minReps.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = repsLineColor,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 44.dp),
            ) {
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                )
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.labelSmall,
                    color = repsLineColor,
                )
            }

            // First / last labels + date labels
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp), // align with chart area
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (firstWeight != null || firstReps != null) {
                        Text(
                            buildString {
                                firstWeight?.let { append(formatWeight(it)) }
                                if (firstWeight != null && firstReps != null) append(" • ")
                                firstReps?.let { append("$it reps") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        formatDate(sessionProgress.first().sessionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if ((lastWeight != null || lastReps != null) && (lastWeight != firstWeight || lastReps != firstReps)) {
                        Text(
                            buildString {
                                lastWeight?.let { append(formatWeight(it)) }
                                if (lastWeight != null && lastReps != null) append(" • ")
                                lastReps?.let { append("$it reps") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        formatDate(sessionProgress.last().sessionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val bestWeight = sessionProgress.mapNotNull { it.bestWeight }.maxOrNull()?.takeIf { it > 0f }
    val isBodyweight = bestWeight == null && sessionProgress.any { it.bodyweightSetCount > 0 }

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
            val bestLabel = when {
                bestWeight != null -> formatWeight(bestWeight)
                isBodyweight -> "BW"
                else -> "—"
            }
            Text(
                text = "${sessionProgress.size} session${if (sessionProgress.size != 1) "s" else ""}" +
                    " • Best $bestLabel",
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val sessionBestWeight = progress.bestWeight
                val sessionBestLabel = when {
                    sessionBestWeight != null -> formatWeight(sessionBestWeight)
                    progress.bodyweightSetCount > 0 -> "BW"
                    else -> "—"
                }
                val statsText = "${progress.setCount} set${if (progress.setCount != 1) "s" else ""}" +
                    " • Best $sessionBestLabel"
                val statsAnnotated = buildAnnotatedString {
                    var i = 0
                    while (i < statsText.length) {
                        if (statsText[i].isDigit()) {
                            val start = i
                            while (i < statsText.length && (statsText[i].isDigit() || statsText[i] == '.')) i++
                            withStyle(SpanStyle(color = primaryColor)) { append(statsText.substring(start, i)) }
                        } else {
                            append(statsText[i])
                            i++
                        }
                    }
                }
                Text(
                    text = statsAnnotated,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "View workout details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RenameMovementDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename movement") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != currentName,
            ) { Text("Rename everywhere") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
