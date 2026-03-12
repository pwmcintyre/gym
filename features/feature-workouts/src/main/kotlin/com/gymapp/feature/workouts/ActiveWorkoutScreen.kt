package com.gymapp.feature.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val bodyWeightKg by viewModel.bodyWeightKg.collectAsStateWithLifecycle()
    val knownNames by viewModel.knownExerciseNames.collectAsStateWithLifecycle()
    val lastPerformance by viewModel.lastPerformance.collectAsStateWithLifecycle()
    var showAddExerciseDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val name = session?.notes?.takeIf { it.isNotBlank() }
                    Text(name ?: "Workout")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            item {
                WorkoutNotesField(
                    notes = session?.notes ?: "",
                    onDone = { viewModel.updateNotes(it) },
                )
            }
            if (exercises.isEmpty()) {
                item {
                    Text(
                        "No exercises yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    bodyWeightKg = bodyWeightKg,
                    previousSets = lastPerformance[exercise.exerciseName],
                    knownNames = knownNames,
                    viewModel = viewModel,
                )
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            nextLabel = nextLabel(exercises),
            knownNames = knownNames,
            onConfirm = { label, name, sets, reps ->
                viewModel.addExercise(label, name, sets, reps)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseEntry,
    bodyWeightKg: Float?,
    previousSets: List<SetEntry>?,
    knownNames: List<String>,
    viewModel: ActiveWorkoutViewModel,
) {
    val sets by viewModel.observeSets(exercise.id).collectAsStateWithLifecycle()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameExerciseDialog(
            current = exercise.exerciseName,
            knownNames = knownNames,
            onConfirm = { newName ->
                viewModel.renameExercise(exercise, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove exercise?") },
            text = { Text("This removes \"${exercise.exerciseName}\" and all its sets.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeExercise(exercise.id)
                    showDeleteConfirm = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(36.dp),
                )
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Rename exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val target = buildString {
                if (exercise.targetSets != null || exercise.targetReps != null) {
                    append("Target: ")
                    exercise.targetSets?.let { append("${it}x") }
                    exercise.targetReps?.let { append("$it") }
                        ?: append("MAX")
                }
            }
            if (target.isNotBlank()) {
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!previousSets.isNullOrEmpty()) {
                val hint = previousSets.joinToString("  ") { set ->
                    buildString {
                        set.weight?.let { append("${formatWeight(it)}kg") }
                        if (set.weight != null && set.repsPerformed != null) append("×")
                        set.repsPerformed?.let { append("$it") }
                    }.ifBlank { "—" }
                }
                Text(
                    text = "↑ $hint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Set rows header
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

            sets.forEach { set ->
                SetRow(set = set, bodyWeightKg = bodyWeightKg, onUpdate = { viewModel.updateSet(it) })
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val last = sets.lastOrNull()
                    viewModel.addSet(
                        exerciseId = exercise.id,
                        setNumber = sets.size + 1,
                        weight = last?.weight,
                        reps = null,
                        weightMode = last?.weightMode ?: WeightMode.BARBELL,
                    )
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun SetRow(set: SetEntry, bodyWeightKg: Float?, onUpdate: (SetEntry) -> Unit) {
    var weightText by remember(set.id) { mutableStateOf(set.weight?.let { formatWeight(it) } ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.repsPerformed?.toString() ?: "") }

    // Sync weightText when mode changes to/from BODYWEIGHT
    val currentMode = set.weightMode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Mode chips row
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.width(36.dp))
            WeightModeChip("BB", currentMode == WeightMode.BARBELL) {
                onUpdate(set.copy(weightMode = WeightMode.BARBELL))
            }
            WeightModeChip("BW", currentMode == WeightMode.BODYWEIGHT) {
                val bwWeight = bodyWeightKg
                if (bwWeight != null) weightText = formatWeight(bwWeight)
                onUpdate(set.copy(weightMode = WeightMode.BODYWEIGHT, weight = bodyWeightKg))
            }
            WeightModeChip("Band", currentMode == WeightMode.BANDED) {
                weightText = ""
                onUpdate(set.copy(weightMode = WeightMode.BANDED, weight = null))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.width(36.dp),
            )
            val weightLabel = when (currentMode) {
                WeightMode.BODYWEIGHT -> "BW kg"
                WeightMode.BANDED -> "band"
                WeightMode.BARBELL -> "kg"
            }
            OutlinedTextField(
                value = weightText,
                onValueChange = { v ->
                    weightText = v
                    onUpdate(set.copy(weight = v.toFloatOrNull()))
                },
                placeholder = { Text(weightLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                readOnly = currentMode == WeightMode.BODYWEIGHT,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { v ->
                    repsText = v
                    onUpdate(set.copy(repsPerformed = v.toIntOrNull()))
                },
                placeholder = { Text("reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeightModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun AddExerciseDialog(
    nextLabel: String,
    knownNames: List<String>,
    onConfirm: (label: String, name: String, sets: Int?, reps: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by rememberSaveable { mutableStateOf(nextLabel) }
    var name by rememberSaveable { mutableStateOf("") }
    var setsText by rememberSaveable { mutableStateOf("") }
    var repsText by rememberSaveable { mutableStateOf("") }

    val suggestions = remember(name, knownNames) {
        if (name.isBlank()) emptyList()
        else knownNames.filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. A1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { name = suggestion },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it },
                        label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            label.ifBlank { nextLabel },
                            name.trim(),
                            setsText.toIntOrNull(),
                            repsText.toIntOrNull(),
                        )
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RenameExerciseDialog(
    current: String,
    knownNames: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(current) }
    val suggestions = remember(name, knownNames) {
        if (name.isBlank()) emptyList()
        else knownNames.filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { name = suggestion },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun WorkoutNotesField(notes: String, onDone: (String) -> Unit) {
    var draft by rememberSaveable(notes) { mutableStateOf(notes) }
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Workout name / notes") },
        placeholder = { Text("e.g. Legs — heavy day") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onDone(draft)
            focusManager.clearFocus()
        }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Generate next label like A1, A2 … B1, B2 … */
private fun nextLabel(exercises: List<ExerciseEntry>): String {
    val last = exercises.lastOrNull()?.label ?: return "A1"
    val letter = last.firstOrNull { it.isLetter() } ?: 'A'
    val num = last.filter { it.isDigit() }.toIntOrNull() ?: 1
    return if (num < 3) "$letter${num + 1}" else "${letter + 1}1"
}

private fun formatWeight(weight: Float): String =
    if (weight == weight.toLong().toFloat()) weight.toLong().toString() else weight.toString()
