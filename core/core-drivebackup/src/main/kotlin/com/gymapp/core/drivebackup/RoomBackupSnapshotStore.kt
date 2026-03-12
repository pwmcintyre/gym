package com.gymapp.core.drivebackup

import androidx.room.withTransaction
import com.gymapp.core.database.GymDatabase
import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.ExerciseTemplateDao
import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.database.entity.ExerciseTemplateEntity
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.database.entity.WorkoutSessionEntity
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.ExerciseTemplate
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBackupSnapshotStore @Inject constructor(
    private val database: GymDatabase,
    private val templateDao: ExerciseTemplateDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val exerciseEntryDao: ExerciseEntryDao,
    private val setEntryDao: SetEntryDao,
) : BackupSnapshotStore {

    override suspend fun exportSnapshot(): Result<BackupSnapshot> = runCatching {
        BackupSnapshot(
            exportedAtEpochMillis = System.currentTimeMillis(),
            exerciseTemplates = templateDao.getAllForBackup().map { it.toModel() },
            workoutSessions = workoutSessionDao.getAllForBackup().map { it.toModel() },
            exerciseEntries = exerciseEntryDao.getAllForBackup().map { it.toModel() },
            setEntries = setEntryDao.getAllForBackup().map { it.toModel() },
        )
    }

    override suspend fun importSnapshot(snapshot: BackupSnapshot): Result<Unit> = runCatching {
        require(snapshot.schemaVersion == BackupSnapshot.CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema version: ${snapshot.schemaVersion}"
        }

        database.withTransaction {
            setEntryDao.deleteAll()
            exerciseEntryDao.deleteAll()
            workoutSessionDao.deleteAll()
            templateDao.deleteAll()

            templateDao.insertAll(snapshot.exerciseTemplates.map { it.toEntity() })
            workoutSessionDao.insertAll(snapshot.workoutSessions.map { it.toEntity() })
            exerciseEntryDao.insertAll(snapshot.exerciseEntries.map { it.toEntity() })
            setEntryDao.insertAll(snapshot.setEntries.map { it.toEntity() })
        }
    }
}

private fun ExerciseTemplateEntity.toModel() = ExerciseTemplate(
    id = id,
    name = name,
    defaultNotes = defaultNotes,
)

private fun ExerciseTemplate.toEntity() = ExerciseTemplateEntity(
    id = id,
    name = name,
    defaultNotes = defaultNotes,
)

private fun WorkoutSessionEntity.toModel() = WorkoutSession(
    id = id,
    date = date,
    notes = notes,
    createdAt = createdAt,
)

private fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    date = date,
    notes = notes,
    createdAt = createdAt,
)

private fun ExerciseEntryEntity.toModel() = ExerciseEntry(
    id = id,
    workoutSessionId = workoutSessionId,
    label = label,
    exerciseName = exerciseName,
    targetSets = targetSets,
    targetReps = targetReps,
    targetModifier = targetModifier,
    targetRawText = targetRawText,
    notes = notes,
)

private fun ExerciseEntry.toEntity() = ExerciseEntryEntity(
    id = id,
    workoutSessionId = workoutSessionId,
    label = label,
    exerciseName = exerciseName,
    targetSets = targetSets,
    targetReps = targetReps,
    targetModifier = targetModifier,
    targetRawText = targetRawText,
    notes = notes,
)

private fun SetEntryEntity.toModel() = SetEntry(
    id = id,
    exerciseEntryId = exerciseEntryId,
    setNumber = setNumber,
    repsPerformed = repsPerformed,
    weight = weight,
    notes = notes,
)

private fun SetEntry.toEntity() = SetEntryEntity(
    id = id,
    exerciseEntryId = exerciseEntryId,
    setNumber = setNumber,
    repsPerformed = repsPerformed,
    weight = weight,
    notes = notes,
)
