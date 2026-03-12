package com.gymapp.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    val driveBackupState by viewModel.driveBackupState.collectAsStateWithLifecycle()
    var keyDraft by rememberSaveable(savedKey) { mutableStateOf(savedKey) }
    var bodyWeightDraft by rememberSaveable(savedBodyWeight) {
        mutableStateOf(savedBodyWeight?.let { formatWeight(it) } ?: "")
    }
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
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
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
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    label = { Text("OpenAI API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.saveApiKey(keyDraft)
                            focusManager.clearFocus()
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Saved when you press Done on the keyboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Saved when you press Done on the keyboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    text = "1.0 — Milestone 6 alpha",
                    style = MaterialTheme.typography.bodyMedium,
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
