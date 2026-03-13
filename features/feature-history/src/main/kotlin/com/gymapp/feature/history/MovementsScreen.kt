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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight

@Composable
fun MovementsScreen(
    onOpenProgress: (exerciseName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = hiltViewModel(),
) {
    val movements by viewModel.movements.collectAsStateWithLifecycle()

    if (movements.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize(),
        ) {
            Text(
                "No movements logged yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(movements, key = { it.exerciseName }) { movement ->
                MovementCard(
                    movement = movement,
                    onClick = { onOpenProgress(movement.exerciseName) },
                )
            }
        }
    }
}

@Composable
private fun MovementCard(
    movement: ExerciseProgressSummary,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = movement.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val primaryColor = MaterialTheme.colorScheme.primary
            val summaryText = buildSummaryLine(movement)
            val summaryAnnotated = buildAnnotatedString {
                var i = 0
                while (i < summaryText.length) {
                    if (summaryText[i].isDigit()) {
                        val start = i
                        while (i < summaryText.length && (summaryText[i].isDigit() || summaryText[i] == '.')) i++
                        withStyle(SpanStyle(color = primaryColor)) { append(summaryText.substring(start, i)) }
                    } else {
                        append(summaryText[i])
                        i++
                    }
                }
            }
            Text(
                text = summaryAnnotated,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = buildAnnotatedString {
                    append("Last logged ")
                    withStyle(SpanStyle(color = primaryColor)) {
                        append(formatDate(movement.lastPerformed))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun buildSummaryLine(movement: ExerciseProgressSummary): String = buildString {
    append("${movement.sessionCount} session")
    if (movement.sessionCount != 1) append("s")
    val bw = movement.bestWeight
    val bestLabel = when {
        bw != null -> formatWeight(bw)
        movement.bodyweightSetCount > 0 -> "BW"
        else -> "—"
    }
    append(" • Best $bestLabel")
}
