package com.gymapp.feature.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import com.gymapp.core.model.findFuzzyMatches
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
    aiViewModel: AiAssistantViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val bodyWeightKg by viewModel.bodyWeightKg.collectAsStateWithLifecycle()
    val knownNames by viewModel.knownExerciseNames.collectAsStateWithLifecycle()
    val lastPerformance by viewModel.lastPerformance.collectAsStateWithLifecycle()
    val lastPerformanceDate by viewModel.lastPerformanceDate.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val isSuggesting by viewModel.isSuggesting.collectAsStateWithLifecycle()
    val isNaming by viewModel.isNaming.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddExerciseDialog by rememberSaveable { mutableStateOf(false) }
    var showAiSheet by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Configure AI assistant with session context
    val sessionId = session?.id
    LaunchedEffect(sessionId) {
        if (sessionId != null) aiViewModel.configure(sessionId = sessionId, workoutMode = true)
    }

    // Vibrate when timer expires
    LaunchedEffect(restTimer) {
        if (restTimer is RestTimerState.Running) {
            val running = restTimer as RestTimerState.Running
            if (running.remainingSeconds == 0) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(android.os.Vibrator::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(400, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(400)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = {
                    if (isNaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(session?.notes?.takeIf { it.isNotBlank() } ?: "Workout")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FloatingActionButton(
                    onClick = { showAiSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant")
                }
                FloatingActionButton(
                    onClick = { showAddExerciseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add exercise")
                }
            }
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()

        // Determine if the exercise card with the active rest timer is currently visible
        val activeTimerExerciseId = (restTimer as? RestTimerState.Running)?.exerciseId
        val timerCardVisible by remember(activeTimerExerciseId, exercises) {
            derivedStateOf {
                if (activeTimerExerciseId == null) true
                else {
                    // exercises are at indices 1..N (index 0 = notes field)
                    val exerciseIndex = exercises.indexOfFirst { it.id == activeTimerExerciseId }
                    if (exerciseIndex < 0) false
                    else {
                        val listIndex = exerciseIndex + 1 // +1 for the notes item
                        listState.layoutInfo.visibleItemsInfo.any { it.index == listIndex }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            // Sticky rest timer banner — shown only when the timer card is scrolled off-screen
            val runningTimer = restTimer as? RestTimerState.Running
            if (runningTimer != null && !timerCardVisible) {
                RestTimerBanner(
                    timer = runningTimer,
                    onCancel = { viewModel.cancelRestTimer() },
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = session?.date?.let { formatDate(it) } ?: "Set date",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (exercises.isEmpty()) {
                    item {
                        WorkoutSuggestionPanel(
                            isSuggesting = isSuggesting,
                            onSuggest = { viewModel.suggestWorkout(it) },
                        )
                    }
                }
                items(exercises, key = { it.id }) { exercise ->
                    val timerForThisExercise = (restTimer as? RestTimerState.Running)
                        ?.takeIf { it.exerciseId == exercise.id }
                    ExerciseCard(
                        exercise = exercise,
                        bodyWeightKg = bodyWeightKg,
                        previousSets = lastPerformance[exercise.exerciseName],
                        previousSessionDate = lastPerformanceDate[exercise.exerciseName],
                        knownNames = knownNames,
                        restTimer = timerForThisExercise,
                        onCancelTimer = { viewModel.cancelRestTimer() },
                        viewModel = viewModel,
                    )
                }
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

    if (showAiSheet) {
        AiAssistantSheet(
            onDismiss = { showAiSheet = false },
            viewModel = aiViewModel,
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = session?.date
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseEntry,
    bodyWeightKg: Float?,
    previousSets: List<SetEntry>?,
    previousSessionDate: Long?,
    knownNames: List<String>,
    restTimer: RestTimerState.Running?,
    onCancelTimer: () -> Unit,
    viewModel: ActiveWorkoutViewModel,
) {
    val sets by viewModel.observeSets(exercise.id).collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditExerciseDialog(
            exercise = exercise,
            knownNames = knownNames,
            onConfirm = { updated ->
                viewModel.updateExercise(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
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
                IconButton(onClick = { showEditDialog = true }) {
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

            if (!previousSets.isNullOrEmpty()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                // Pick best set: highest weight, or most reps for bodyweight
                val best = previousSets.maxWithOrNull(
                    compareBy({ it.weight ?: 0f }, { it.repsPerformed ?: 0 })
                )
                if (best != null) {
                    val timeAgo = previousSessionDate?.let { relativeTimeLabel(it) }
                    val prevText = buildAnnotatedString {
                        append("Previous: ")
                        best.repsPerformed?.let {
                            withStyle(SpanStyle(color = primaryColor)) { append("$it") }
                            append(" reps")
                        }
                        best.weight?.let {
                            append(" × ")
                            withStyle(SpanStyle(color = primaryColor)) { append(formatWeight(it)) }
                        }
                        if (timeAgo != null) {
                            append(" (")
                            withStyle(SpanStyle(color = primaryColor)) { append(timeAgo) }
                            append(")")
                        }
                    }
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (restTimer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                RestTimerRow(timer = restTimer, onCancel = onCancelTimer)
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
                SetRow(
                    set = set,
                    bodyWeightKg = bodyWeightKg,
                    onUpdate = { viewModel.updateSet(it) },
                    onDelete = { viewModel.deleteSet(set.id) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val last = sets.lastOrNull()
                    viewModel.addSet(
                        exerciseId = exercise.id,
                        setNumber = sets.size + 1,
                        weight = last?.weight,
                        reps = last?.repsPerformed,
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
private fun SetRow(set: SetEntry, bodyWeightKg: Float?, onUpdate: (SetEntry) -> Unit, onDelete: () -> Unit) {
    var weightText by remember(set.id, set.weight) { mutableStateOf(set.weight?.let { formatWeight(it, appendUnit = false) } ?: "") }
    var repsText by remember(set.id, set.repsPerformed) { mutableStateOf(set.repsPerformed?.toString() ?: "") }

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
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

    val trimmedName = name.trim()
    val suggestions = remember(trimmedName, knownNames) {
        if (trimmedName.isBlank()) emptyList()
        else knownNames.filter { it.contains(trimmedName, ignoreCase = true) && !it.equals(trimmedName, ignoreCase = true) }.take(4)
    }
    val fuzzySuggestions = remember(trimmedName, knownNames) {
        if (trimmedName.isBlank() || suggestions.isNotEmpty()) emptyList()
        else findFuzzyMatches(trimmedName, knownNames, maxResults = 3)
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
                if (fuzzySuggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item {
                            Text(
                                "Similar:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp, top = 6.dp),
                            )
                        }
                        items(fuzzySuggestions) { suggestion ->
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
private fun EditExerciseDialog(
    exercise: ExerciseEntry,
    knownNames: List<String>,
    onConfirm: (ExerciseEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(exercise.exerciseName) }
    var setsText by rememberSaveable { mutableStateOf(exercise.targetSets?.toString() ?: "") }
    var repsText by rememberSaveable { mutableStateOf(exercise.targetReps?.toString() ?: "") }
    var isAmrap by rememberSaveable { mutableStateOf(exercise.targetModifier == RepModifier.MAX) }
    var notes by rememberSaveable { mutableStateOf(exercise.notes ?: "") }

    val suggestions = remember(name, knownNames) {
        if (name.isBlank()) emptyList()
        else knownNames.filter { it.contains(name.trim(), ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit exercise") },
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
                        enabled = !isAmrap,
                        modifier = Modifier.weight(1f),
                    )
                }
                FilterChip(
                    selected = isAmrap,
                    onClick = { isAmrap = !isAmrap },
                    label = { Text("AMRAP") },
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(
                        exercise.copy(
                            exerciseName = name.trim(),
                            targetSets = setsText.toIntOrNull(),
                            targetReps = if (isAmrap) null else repsText.toIntOrNull(),
                            targetModifier = if (isAmrap) RepModifier.MAX else RepModifier.NONE,
                            notes = notes.takeIf { it.isNotBlank() },
                        )
                    )
                }
            }) { Text("Save") }
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
        label = { Text("Workout name") },
        placeholder = { Text("e.g. Legs — heavy day") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onDone(draft)
            focusManager.clearFocus()
        }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused) onDone(draft)
            },
    )
}

@Composable
private fun RestTimerRow(timer: RestTimerState.Running, onCancel: () -> Unit) {
    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else 0f
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60
    val timeText = "%d:%02d".format(minutes, seconds)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "Rest",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancel rest timer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact banner shown at the top of the screen when the rest timer card is scrolled out of view. */
@Composable
private fun RestTimerBanner(timer: RestTimerState.Running, onCancel: () -> Unit) {
    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else 0f
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60
    val timeText = "%d:%02d".format(minutes, seconds)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
            Text(
                text = "Resting $timeText",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel rest timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Human-readable relative time label from an epoch ms timestamp to now. */
private fun relativeTimeLabel(epochMs: Long): String {
    val diffMs = System.currentTimeMillis() - epochMs
    val days = (diffMs / (1000L * 60 * 60 * 24)).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        days < 14 -> "$days days ago"
        days < 60 -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}

/** Generate next label like A1, A2 … B1, B2 … */
private fun nextLabel(exercises: List<ExerciseEntry>): String {
    val last = exercises.lastOrNull()?.label ?: return "A1"
    val letter = last.firstOrNull { it.isLetter() } ?: 'A'
    val num = last.filter { it.isDigit() }.toIntOrNull() ?: 1
    return if (num < 3) "$letter${num + 1}" else "${letter + 1}1"
}

@Composable
private fun WorkoutSuggestionPanel(
    isSuggesting: Boolean,
    onSuggest: (SuggestionType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    ) {
        Text(
            "No exercises yet. Tap + to add, or let AI suggest a starting point:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isSuggesting) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionButton("Double down", "Continue & intensify recent training") {
                    onSuggest(SuggestionType.DOUBLE_DOWN)
                }
                SuggestionButton("Fill the gaps", "Train what you've been skipping") {
                    onSuggest(SuggestionType.FILL_GAPS)
                }
                SuggestionButton("Cardio", "Conditioning — no weights") {
                    onSuggest(SuggestionType.CARDIO)
                }
            }
        }
    }
}

@Composable
private fun SuggestionButton(label: String, subtitle: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
