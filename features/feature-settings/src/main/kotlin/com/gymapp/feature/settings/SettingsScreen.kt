package com.gymapp.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.gymapp.core.ai.ModelState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val savedKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val savedBodyWeight by viewModel.bodyWeightKg.collectAsStateWithLifecycle()
    val savedRestTimer by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val savedTrainingConstraints by viewModel.trainingConstraints.collectAsStateWithLifecycle()
    val driveBackupState by viewModel.driveBackupState.collectAsStateWithLifecycle()
    val lastAiError by viewModel.lastAiError.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    var keyDraft by rememberSaveable(savedKey) { mutableStateOf(savedKey) }
    var bodyWeightDraft by rememberSaveable(savedBodyWeight) {
        mutableStateOf(savedBodyWeight?.let { formatWeight(it) } ?: "")
    }
    var settingsDestination by rememberSaveable { mutableStateOf(SettingsDestination.Root) }
    var showRestoreConfirm by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleDriveSignInResult(result.data)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (settingsDestination == SettingsDestination.TrainingConstraints) {
                TopAppBar(
                    title = { Text("Training Constraints") },
                    navigationIcon = {
                        IconButton(onClick = { settingsDestination = SettingsDestination.Root }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        when (settingsDestination) {
            SettingsDestination.Root -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SettingsSectionCard(
                        title = "AI Scan",
                        supportingText = "Your OpenAI API key. Get one at platform.openai.com. Stored only on this device.",
                    ) {
                        lastAiError?.let { error ->
                            Text(
                                text = "Last error: $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedTextField(
                            value = keyDraft,
                            onValueChange = {
                                keyDraft = it
                                viewModel.saveApiKey(it)
                            },
                            label = { Text("OpenAI API Key") },
                            placeholder = { Text("sk-...") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                                text = "Saved as you type.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        LocalModelCard(
                            modelState = modelState,
                            hasApiKey = savedKey.isNotBlank(),
                            onDownload = viewModel::downloadModel,
                            onDelete = viewModel::deleteModel,
                        )

                        SettingsSectionCard(
                            title = "Training Constraints",
                        supportingText = savedTrainingConstraints.previewConstraints()
                            .takeIf { it.isNotBlank() }
                            ?: "Tell the coach about your equipment, injuries, or limits.",
                    ) {
                        Button(
                            onClick = { settingsDestination = SettingsDestination.TrainingConstraints },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit Constraints")
                        }
                    }

                    SettingsSectionCard(
                        title = "Body Weight",
                        supportingText = "Your current body weight. Used as a weight value when you tap BW during set entry.",
                    ) {
                        OutlinedTextField(
                            value = bodyWeightDraft,
                            onValueChange = { bodyWeightDraft = it },
                            label = { Text("Body weight (kg)") },
                            placeholder = { Text("e.g. 80") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    bodyWeightDraft.toFloatOrNull()?.let { viewModel.saveBodyWeight(it) }
                                    focusManager.clearFocus()
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        bodyWeightDraft.toFloatOrNull()?.let { viewModel.saveBodyWeight(it) }
                                    }
                                },
                        )
                        Text(
                            text = "Saved automatically when you leave the field.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SettingsSectionCard(
                        title = "Rest Timer",
                        supportingText = "How long to rest between sets. The timer starts automatically after each set.",
                    ) {
                        val presets = listOf(60, 90, 120, 180)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = savedRestTimer == preset,
                                    onClick = { viewModel.saveRestTimerSeconds(preset) },
                                    label = {
                                        Text(
                                            if (preset < 60) "${preset}s" else "${preset / 60}m${if (preset % 60 != 0) "${preset % 60}s" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    SettingsSectionCard(
                        title = "Cloud Backup",
                        supportingText = "Back up workouts to Google Drive appDataFolder. Restore replaces local workout data. OpenAI API keys stay local.",
                    ) {
                        Text(
                            text = if (driveBackupState.isAuthorized) {
                                "Connected: ${driveBackupState.accountLabel ?: "Google account"}"
                            } else {
                                "Not connected to Google Drive."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (driveBackupState.isAuthorized) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearDriveStatus()
                                    driveSignInLauncher.launch(viewModel.createDriveSignInIntent())
                                },
                                enabled = !driveBackupState.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    if (driveBackupState.isAuthorized) {
                                        "Reconnect Google Drive"
                                    } else {
                                        "Connect Google Drive"
                                    },
                                )
                            }
                            Button(
                                onClick = viewModel::backupNow,
                                enabled = driveBackupState.isAuthorized && !driveBackupState.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Back Up Now")
                            }
                            OutlinedButton(
                                onClick = { showRestoreConfirm = true },
                                enabled = driveBackupState.isAuthorized && !driveBackupState.isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Restore Backup")
                            }
                        }
                        if (driveBackupState.isWorking) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        driveBackupState.statusMessage?.let { message ->
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    SettingsSectionCard(title = "Version") {
                        Text(
                            text = "1.0 — Milestone 10",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            SettingsDestination.TrainingConstraints -> {
                TrainingConstraintsScreen(
                    savedValue = savedTrainingConstraints,
                    onSave = viewModel::saveTrainingConstraints,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore backup?") },
            text = {
                Text("This replaces local workout data with the latest Google Drive backup.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreNow()
                    },
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private enum class SettingsDestination {
    Root,
    TrainingConstraints,
}

@Composable
private fun TrainingConstraintsScreen(
    savedValue: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(savedValue) { mutableStateOf(savedValue) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "These notes are sent with every coach request.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                onSave(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            label = { Text("Training constraints") },
            placeholder = {
                Text(
                    "I train at home with dumbbells up to 30kg and a pull-up bar. I have a left shoulder impingement, avoid overhead pressing.",
                )
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Saved automatically as you type.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                onSave(draft)
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun LocalModelCard(
    modelState: ModelState,
    hasApiKey: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    SettingsSectionCard(
        title = "Offline AI (Qwen3 0.6B)",
        supportingText = if (hasApiKey) {
            "OpenAI is active. The local model is used when no API key is set."
        } else {
            "No API key set — the app uses the local Qwen3 model for AI features."
        },
    ) {
        when (modelState) {
            is ModelState.NotDownloaded -> {
                Text(
                    text = "Model not downloaded (~614 MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text("Download Qwen3 0.6B")
                }
            }

            is ModelState.Downloading -> {
                val progress = modelState.progress
                Text(
                    text = if (progress >= 0) "Downloading… $progress%" else "Downloading…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress >= 0) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            is ModelState.Ready -> {
                Text(
                    text = "Qwen3 0.6B ready for offline use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete model (free storage)")
                }
            }

            is ModelState.Error -> {
                Text(
                    text = "Error: ${modelState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry Download")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

private fun formatWeight(weight: Float): String =
    if (weight == weight.toLong().toFloat()) weight.toLong().toString() else weight.toString()

private fun String.previewConstraints(maxChars: Int = 120): String {
    val singleLine = lineSequence().joinToString(" ").replace(Regex("\\s+"), " ").trim()
    if (singleLine.length <= maxChars) return singleLine
    return singleLine.take(maxChars - 1).trimEnd() + "…"
}
