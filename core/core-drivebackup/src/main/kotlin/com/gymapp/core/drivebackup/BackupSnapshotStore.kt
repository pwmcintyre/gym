package com.gymapp.core.drivebackup

interface BackupSnapshotStore {
    suspend fun exportSnapshot(): Result<BackupSnapshot>

    suspend fun importSnapshot(snapshot: BackupSnapshot): Result<Unit>
}
