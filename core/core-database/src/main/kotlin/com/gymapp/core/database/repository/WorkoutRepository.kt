package com.gymapp.core.database.repository

import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.database.entity.WorkoutSessionEntity
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.ExerciseSessionProgress
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SessionSummary
import com.gymapp.core.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val sessionDao: WorkoutSessionDao,
    private val exerciseDao: ExerciseEntryDao,
) {
    fun observeAll(): Flow<List<WorkoutSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeById(id: String): Flow<WorkoutSession?> =
        sessionDao.observeById(id).map { it?.toModel() }

    suspend fun getById(id: String): WorkoutSession? =
        sessionDao.getById(id)?.toModel()

    suspend fun create(date: Long, notes: String? = null): WorkoutSession {
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            date = date,
            notes = notes,
            createdAt = System.currentTimeMillis(),
        )
        sessionDao.insert(session.toEntity())
        return session
    }

    suspend fun update(session: WorkoutSession) =
        sessionDao.update(session.toEntity())

    suspend fun delete(id: String) =
        sessionDao.deleteById(id)

    fun observeExercises(sessionId: String): Flow<List<ExerciseEntry>> =
        exerciseDao.observeBySession(sessionId).map { list -> list.map { it.toModel() } }

    fun observeProgressSummaries(limit: Int = 5): Flow<List<ExerciseProgressSummary>> =
        exerciseDao.observeProgressSummaries(limit)

    fun observeProgressSessions(exerciseName: String): Flow<List<ExerciseSessionProgress>> =
        exerciseDao.observeProgressSessions(exerciseName)

    fun observeDistinctExerciseNames(): Flow<List<String>> =
        exerciseDao.observeDistinctNames()

    fun observeSessionSummaries(): Flow<Map<String, SessionSummary>> =
        exerciseDao.observeSessionSummaries()
            .map { list -> list.associateBy { it.sessionId } }

    suspend fun addExercise(
        sessionId: String,
        label: String,
        exerciseName: String,
        targetSets: Int? = null,
        targetReps: Int? = null,
        targetModifier: RepModifier = RepModifier.NONE,
        targetRawText: String? = null,
        notes: String? = null,
    ): ExerciseEntry {
        val entry = ExerciseEntry(
            id = UUID.randomUUID().toString(),
            workoutSessionId = sessionId,
            label = label,
            exerciseName = exerciseName,
            targetSets = targetSets,
            targetReps = targetReps,
            targetModifier = targetModifier,
            targetRawText = targetRawText,
            notes = notes,
        )
        exerciseDao.insert(entry.toEntity())
        return entry
    }

    suspend fun updateExercise(entry: ExerciseEntry) =
        exerciseDao.update(entry.toEntity())

    suspend fun deleteExercise(id: String) =
        exerciseDao.deleteById(id)

    /** Copies exercises (label, name, targets) from [sourceSessionId] into [destSessionId]. */
    suspend fun copyExercisesFromSession(sourceSessionId: String, destSessionId: String) {
        val source = exerciseDao.observeBySession(sourceSessionId).first()
        source.forEach { entity ->
            exerciseDao.insert(
                entity.copy(
                    id = UUID.randomUUID().toString(),
                    workoutSessionId = destSessionId,
                )
            )
        }
    }
}

private fun WorkoutSessionEntity.toModel() = WorkoutSession(id, date, notes, createdAt)
private fun WorkoutSession.toEntity() = WorkoutSessionEntity(id, date, notes, createdAt)

private fun ExerciseEntryEntity.toModel() = ExerciseEntry(
    id, workoutSessionId, label, exerciseName,
    targetSets, targetReps, targetModifier, targetRawText, notes,
)

private fun ExerciseEntry.toEntity() = ExerciseEntryEntity(
    id, workoutSessionId, label, exerciseName,
    targetSets, targetReps, targetModifier, targetRawText, notes,
)
