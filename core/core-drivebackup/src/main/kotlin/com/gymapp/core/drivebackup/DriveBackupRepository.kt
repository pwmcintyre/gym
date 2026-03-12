package com.gymapp.core.drivebackup

/**
 * Scaffold — Google Drive appDataFolder backup repository.
 *
 * Implementation is deferred to a future milestone.
 * This interface defines the contract so other modules can depend on it
 * without coupling to the implementation.
 */
interface DriveBackupRepository {

    /**
     * Upload the local database snapshot to Drive appDataFolder.
     * @return true on success.
     */
    suspend fun backup(): Result<Unit>

    /**
     * Restore the most recent backup from Drive appDataFolder.
     * @return true if a backup was found and restored.
     */
    suspend fun restore(): Result<Unit>
}
