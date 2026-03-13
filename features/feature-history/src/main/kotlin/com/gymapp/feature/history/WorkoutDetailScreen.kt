package com.gymapp.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import com.gymapp.core.model.buildExerciseSections
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight
import com.gymapp.core.model.labelParts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onEdit: (sessionId: String) -> Unit,
    onOpenProgress: (exerciseName: String) -> Unit,
    onCompare: (workoutName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val exerciseSections = buildExerciseSections(exercises)

    val titleText = session?.let { s ->
        s.notes?.takeIf { it.isNotBlank() } ?: formatDate(s.date)
    } ?: "Workout"

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only show Compare when this session has a name to group by
                    val sessionName = session?.notes?.takeIf { it.isNotBlank() }
                    if (sessionName != null) {
                        IconButton(onClick = { onCompare(sessionName) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Compare movement progress")
                        }
                    }
                    IconButton(onClick = { onEdit(viewModel.sessionId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit workout")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (exercises.isEmpty()) {
                item {
                    Text(
                        "No movements recorded.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
            exerciseSections.forEachIndexed { index, section ->
                if (section.supersetKey != null) {
                    item(key = "superset_header_${section.supersetKey}_$index") {
                        Text(
                            text = "Superset ${section.supersetKey}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(section.exercises, key = { it.id }) { exercise ->
                    ExerciseDetailCard(
                        exercise = exercise,
                        displayLabel = exercise.displayLabel(inSuperset = section.supersetKey != null),
                        viewModel = viewModel,
                        onOpenProgress = { onOpenProgress(exercise.exerciseName) },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete workout?") },
            text = { Text("This will permanently delete this workout session and all its data.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteSession(onBack) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ExerciseDetailCard(
    exercise: ExerciseEntry,
    displayLabel: String?,
    viewModel: WorkoutDetailViewModel,
    onOpenProgress: () -> Unit,
) {
    val sets by viewModel.observeSets(exercise.id).collectAsStateWithLifecycle()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (displayLabel != null) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(36.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenProgress) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = "View movement",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val hasTarget = exercise.targetSets != null ||
                exercise.targetReps != null ||
                exercise.targetModifier == RepModifier.MAX
            if (hasTarget) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val targetText = buildAnnotatedString {
                    append("Target: ")
                    exercise.targetSets?.let {
                        withStyle(SpanStyle(color = primaryColor)) { append("$it") }
                        append(" sets")
                    }
                    if (exercise.targetModifier == RepModifier.MAX) {
                        if (exercise.targetSets != null) append(" × ")
                        withStyle(SpanStyle(color = primaryColor)) { append("AMRAP") }
                    } else {
                        exercise.targetReps?.let {
                            if (exercise.targetSets != null) append(" × ")
                            withStyle(SpanStyle(color = primaryColor)) { append("$it") }
                            append(" reps")
                        }
                    }
                }
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            exercise.notes?.takeIf { it.isNotBlank() }?.let { modifierText ->
                Text(
                    text = "Modifier: $modifierText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Set",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                    )
                    Text(
                        "Weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Reps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                sets.forEach { set -> SetDetailRow(set) }
            }
        }
    }
}

private fun ExerciseEntry.displayLabel(inSuperset: Boolean): String? {
    val parts = labelParts()
    return when {
        inSuperset -> parts.memberLabel
        label.isBlank() -> null
        else -> label
    }
}

@Composable
private fun SetDetailRow(set: SetEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(36.dp),
        )
        val weightLabel = when (set.weightMode) {
            WeightMode.BODYWEIGHT -> set.weight?.let { "BW ${formatWeight(it, appendUnit = false)} kg" } ?: "BW"
            WeightMode.BANDED -> set.weight?.let { "Band ${formatWeight(it, appendUnit = false)}" } ?: "Band"
            WeightMode.BARBELL -> set.weight?.let { formatWeight(it) } ?: "—"
        }
        Text(
            text = weightLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = set.repsPerformed?.toString() ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
