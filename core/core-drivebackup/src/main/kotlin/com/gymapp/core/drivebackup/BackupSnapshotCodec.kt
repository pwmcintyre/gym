package com.gymapp.core.drivebackup

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class BackupSnapshotCodec @Inject constructor() {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(snapshot: BackupSnapshot): ByteArray =
        json.encodeToString(snapshot).toByteArray(StandardCharsets.UTF_8)

    fun decode(bytes: ByteArray): BackupSnapshot {
        val snapshot = json.decodeFromString<BackupSnapshot>(
            bytes.toString(StandardCharsets.UTF_8),
        )
        require(snapshot.schemaVersion == BackupSnapshot.CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema version: ${snapshot.schemaVersion}"
        }
        return snapshot
    }
}
