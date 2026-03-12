package com.gymapp.core.drivebackup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class GoogleDriveBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: GoogleDriveAuthManager,
    private val snapshotStore: BackupSnapshotStore,
    private val snapshotCodec: BackupSnapshotCodec,
) : DriveBackupRepository {

    override suspend fun backup(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = snapshotStore.exportSnapshot().getOrThrow()
            val payload = snapshotCodec.encode(snapshot)
            val drive = createDriveService()
            val existingBackup = findLatestBackupFile(drive)
            val mediaContent = ByteArrayContent("application/json", payload)

            if (existingBackup == null) {
                val metadata = File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf(APP_DATA_FOLDER)
                }
                drive.files()
                    .create(metadata, mediaContent)
                    .setFields("id")
                    .execute()
            } else {
                val metadata = File().apply {
                    name = BACKUP_FILE_NAME
                }
                drive.files()
                    .update(existingBackup.id, metadata, mediaContent)
                    .setFields("id")
                    .execute()
            }

            Unit
        }
    }

    override suspend fun restore(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = createDriveService()
            val remoteBackup = findLatestBackupFile(drive)
                ?: error("No backup found in Google Drive appDataFolder.")

            val output = ByteArrayOutputStream()
            drive.files()
                .get(remoteBackup.id)
                .executeMediaAndDownloadTo(output)

            val snapshot = snapshotCodec.decode(output.toByteArray())
            snapshotStore.importSnapshot(snapshot).getOrThrow()
        }
    }

    private fun createDriveService(): Drive {
        val account = authManager.requireAuthorizedAccount()
        val googleAccount = requireNotNull(account.account) {
            "Selected Google account is unavailable on this device."
        }
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            setOf(DriveScopes.DRIVE_APPDATA),
        ).apply {
            selectedAccount = googleAccount
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential,
        )
            .setApplicationName("GymApp")
            .build()
    }

    private fun findLatestBackupFile(drive: Drive): File? =
        drive.files()
            .list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setOrderBy("modifiedTime desc")
            .setPageSize(1)
            .setFields("files(id,name,modifiedTime,size)")
            .execute()
            .files
            ?.firstOrNull()

    private companion object {
        const val APP_DATA_FOLDER = "appDataFolder"
        const val BACKUP_FILE_NAME = "gymapp-backup-v1.json"
    }
}
