package com.gymapp.core.drivebackup

import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.ExerciseTemplate
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import kotlinx.serialization.Serializable

/**
 * Versioned backup payload for durable cloud storage.
 *
 * The payload intentionally contains workout state only. Secrets such as API keys
 * remain local until there is an explicit product decision to back them up.
 */
@Serializable
data class BackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val exerciseTemplates: List<ExerciseTemplate>,
    val workoutSessions: List<WorkoutSession>,
    val exerciseEntries: List<ExerciseEntry>,
    val setEntries: List<SetEntry>,
) {
    companion object {
        /** v3: ExerciseEntry gains modifierTags. */
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
