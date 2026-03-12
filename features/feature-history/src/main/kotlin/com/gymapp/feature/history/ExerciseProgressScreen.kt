package com.gymapp.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseSessionProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    onBack: () -> Unit,
    onOpenWorkout: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseProgressViewModel = hiltViewModel(),
) {
    val sessionProgress by viewModel.sessionProgress.collectAsStateWithLifecycle()

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

@Composable
private fun ExerciseProgressSummaryCard(
    exerciseName: String,
    sessionProgress: List<ExerciseSessionProgress>,
) {
    val totalVolume = sessionProgress.sumOf { it.totalVolume.toDouble() }.toFloat()
    val bestWeight = sessionProgress.maxOfOrNull { it.bestWeight ?: 0f }?.takeIf { it > 0f }

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
                text = "${sessionProgress.size} sessions • Best ${bestWeight?.let(::formatHistoryWeight) ?: "—"} • Volume ${formatHistoryVolume(totalVolume)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
                text = formatHistoryDate(progress.sessionDate),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${progress.setCount} sets • Best ${progress.bestWeight?.let(::formatHistoryWeight) ?: "—"} • Volume ${formatHistoryVolume(progress.totalVolume)}",
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

private fun formatHistoryDate(epochMillis: Long): String =
    SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date(epochMillis))

private fun formatHistoryWeight(weight: Float): String {
    val value = if (weight == weight.toLong().toFloat()) {
        weight.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", weight)
    }
    return "$value kg"
}

private fun formatHistoryVolume(volume: Float): String {
    val value = if (volume == volume.toLong().toFloat()) {
        volume.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", volume)
    }
    return "$value kg"
}
