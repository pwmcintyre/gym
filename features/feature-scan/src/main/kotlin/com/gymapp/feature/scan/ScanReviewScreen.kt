package com.gymapp.feature.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymapp.core.model.ExerciseEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewScreen(
    items: List<ExerciseEntry>,
    knownNames: List<String>,
    onItemChanged: (index: Int, item: ExerciseEntry) -> Unit,
    onItemRemoved: (index: Int) -> Unit,
    onItemAdded: () -> Unit,
    onStartWorkout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Review Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onStartWorkout,
                enabled = items.any { it.exerciseName.isNotBlank() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Start Workout")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ReviewItemCard(
                    item = item,
                    knownNames = knownNames,
                    onChanged = { onItemChanged(index, it) },
                    onRemove = { onItemRemoved(index) },
                )
            }
            item {
                TextButton(
                    onClick = onItemAdded,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add exercise")
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(
    item: ExerciseEntry,
    knownNames: List<String>,
    onChanged: (ExerciseEntry) -> Unit,
    onRemove: () -> Unit,
) {
    val suggestions = remember(item.exerciseName, knownNames) {
        if (item.exerciseName.isBlank()) emptyList()
        else knownNames.filter {
            it.contains(item.exerciseName.trim(), ignoreCase = true) &&
                !it.equals(item.exerciseName.trim(), ignoreCase = true)
        }.take(5)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = item.label,
                    onValueChange = { onChanged(item.copy(label = it)) },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = item.exerciseName,
                    onValueChange = { onChanged(item.copy(exerciseName = it)) },
                    label = { Text("Exercise") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onChanged(item.copy(exerciseName = suggestion)) },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = item.targetSets?.toString() ?: "",
                    onValueChange = { onChanged(item.copy(targetSets = it.toIntOrNull())) },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = item.targetReps?.toString() ?: "",
                    onValueChange = { onChanged(item.copy(targetReps = it.toIntOrNull())) },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!item.notes.isNullOrBlank()) {
                val notes = item.notes
                Text(
                    text = "Notes: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
