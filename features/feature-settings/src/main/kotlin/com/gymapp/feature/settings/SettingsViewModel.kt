package com.gymapp.feature.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiSettings
import com.gymapp.core.drivebackup.DriveAccountState
import com.gymapp.core.drivebackup.DriveBackupRepository
import com.gymapp.core.drivebackup.GoogleDriveAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriveBackupUiState(
    val accountLabel: String? = null,
    val isAuthorized: Boolean = false,
    val isWorking: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiSettings: AiSettings,
    private val driveAuthManager: GoogleDriveAuthManager,
    private val driveBackupRepository: DriveBackupRepository,
) : ViewModel() {

    val apiKey: StateFlow<String> = aiSettings.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _driveBackupState = MutableStateFlow(
        driveAuthManager.currentAccountState().toUiState(),
    )
    val driveBackupState: StateFlow<DriveBackupUiState> = _driveBackupState.asStateFlow()

    fun saveApiKey(key: String) {
        viewModelScope.launch { aiSettings.setApiKey(key) }
    }

    fun createDriveSignInIntent(): Intent =
        driveAuthManager.createSignInIntent()

    fun handleDriveSignInResult(data: Intent?) {
        val result = driveAuthManager.handleSignInResult(data)
        _driveBackupState.value = result.fold(
            onSuccess = { state ->
                state.toUiState(statusMessage = "Google Drive connected.")
            },
            onFailure = { error ->
                driveAuthManager.currentAccountState().toUiState(
                    statusMessage = error.userMessage(),
                )
            },
        )
    }

    fun backupNow() {
        runDriveOperation(
            successMessage = "Backup saved to Google Drive appDataFolder.",
            operation = { driveBackupRepository.backup() },
        )
    }

    fun restoreNow() {
        runDriveOperation(
            successMessage = "Local data restored from Google Drive.",
            operation = { driveBackupRepository.restore() },
        )
    }

    fun clearDriveStatus() {
        _driveBackupState.value = _driveBackupState.value.copy(statusMessage = null)
    }

    private fun runDriveOperation(
        successMessage: String,
        operation: suspend () -> Result<Unit>,
    ) {
        if (_driveBackupState.value.isWorking) return

        viewModelScope.launch {
            _driveBackupState.value = _driveBackupState.value.copy(
                isWorking = true,
                statusMessage = null,
            )

            val result = operation()
            val latestState = driveAuthManager.currentAccountState()

            _driveBackupState.value = latestState.toUiState(
                isWorking = false,
                statusMessage = result.fold(
                    onSuccess = { successMessage },
                    onFailure = { it.userMessage() },
                ),
            )
        }
    }
}

private fun DriveAccountState.toUiState(
    isWorking: Boolean = false,
    statusMessage: String? = null,
) = DriveBackupUiState(
    accountLabel = accountLabel,
    isAuthorized = isAuthorized,
    isWorking = isWorking,
    statusMessage = statusMessage,
)

private fun Throwable.userMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Google Drive backup failed."
