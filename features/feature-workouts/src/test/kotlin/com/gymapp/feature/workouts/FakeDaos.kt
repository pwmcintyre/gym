package com.gymapp.feature.workouts

import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.database.entity.WorkoutSessionEntity
import com.gymapp.core.model.ExercisePr
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.ExerciseSessionProgress
import com.gymapp.core.model.ExerciseWorkoutProgression
import com.gymapp.core.model.SessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeWorkoutSessionDao : WorkoutSessionDao {
    private val store = mutableListOf<WorkoutSessionEntity>()
    private val _flow = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())

    override fun observeAll(): Flow<List<WorkoutSessionEntity>> = _flow
    override fun observeById(id: String): Flow<WorkoutSessionEntity?> =
        _flow.map { it.find { e -> e.id == id } }
    override suspend fun getById(id: String): WorkoutSessionEntity? =
        store.find { it.id == id }
    override suspend fun getAllForBackup(): List<WorkoutSessionEntity> = store.toList()
    override suspend fun insert(entity: WorkoutSessionEntity) {
        store.removeAll { it.id == entity.id }
        store.add(entity)
        _flow.value = store.toList()
    }
    override suspend fun insertAll(entities: List<WorkoutSessionEntity>) =
        entities.forEach { insert(it) }
    override suspend fun update(entity: WorkoutSessionEntity) {
        store.replaceAll { if (it.id == entity.id) entity else it }
        _flow.value = store.toList()
    }
    override suspend fun delete(entity: WorkoutSessionEntity) {
        store.remove(entity)
        _flow.value = store.toList()
    }
    override suspend fun deleteById(id: String) {
        store.removeAll { it.id == id }
        _flow.value = store.toList()
    }
    override suspend fun deleteAll() {
        store.clear()
        _flow.value = emptyList()
    }
    override fun observeDistinctWorkoutNames(): Flow<List<String>> =
        _flow.map { list -> list.mapNotNull { it.notes }.filter { it.isNotBlank() }.distinct() }
}

class FakeExerciseEntryDao : ExerciseEntryDao {
    private val store = mutableListOf<ExerciseEntryEntity>()
    private val _flow = MutableStateFlow<List<ExerciseEntryEntity>>(emptyList())

    override fun observeBySession(sessionId: String): Flow<List<ExerciseEntryEntity>> =
        _flow.map { list -> list.filter { it.workoutSessionId == sessionId }.sortedBy { it.label } }
    override suspend fun getById(id: String): ExerciseEntryEntity? = store.find { it.id == id }
    override suspend fun getAllForBackup(): List<ExerciseEntryEntity> = store.toList()
    override fun observeProgressSummaries(limit: Int): Flow<List<ExerciseProgressSummary>> =
        flowOf(emptyList())
    override fun observeProgressSessions(exerciseName: String): Flow<List<ExerciseSessionProgress>> =
        flowOf(emptyList())
    override fun observeDistinctNames(): Flow<List<String>> =
        _flow.map { list ->
            list.map { it.exerciseName }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    override fun observeExerciseProgressions(workoutName: String): Flow<List<ExerciseWorkoutProgression>> =
        flowOf(emptyList())
    override fun observeSessionSummaries(): Flow<List<SessionSummary>> =
        _flow.map { list ->
            list.groupBy { it.workoutSessionId }.map { (sessionId, exercises) ->
                SessionSummary(
                    sessionId = sessionId,
                    exerciseCount = exercises.size,
                    setCount = 0,
                    targetSetCount = exercises.sumOf { it.targetSets ?: 0 },
                )
            }
        }
    override suspend fun insert(entity: ExerciseEntryEntity) {
        store.removeAll { it.id == entity.id }
        store.add(entity)
        _flow.value = store.toList()
    }
    override suspend fun insertAll(entities: List<ExerciseEntryEntity>) =
        entities.forEach { insert(it) }
    override suspend fun update(entity: ExerciseEntryEntity) {
        store.replaceAll { if (it.id == entity.id) entity else it }
        _flow.value = store.toList()
    }
    override suspend fun delete(entity: ExerciseEntryEntity) {
        store.remove(entity)
        _flow.value = store.toList()
    }
    override suspend fun deleteById(id: String) {
        store.removeAll { it.id == id }
        _flow.value = store.toList()
    }
    override suspend fun deleteBySession(sessionId: String) {
        store.removeAll { it.workoutSessionId == sessionId }
        _flow.value = store.toList()
    }
    override suspend fun renameGlobally(oldName: String, newName: String) {
        store.replaceAll { if (it.exerciseName == oldName) it.copy(exerciseName = newName) else it }
        _flow.value = store.toList()
    }
    override suspend fun getExerciseNamesForSession(sessionId: String): List<String> =
        store.filter { it.workoutSessionId == sessionId }
            .map { it.exerciseName }
            .filter { it.isNotBlank() }
            .distinct()
    override suspend fun deleteAll() {
        store.clear()
        _flow.value = emptyList()
    }
}

class FakeSetEntryDao : SetEntryDao {
    private val store = mutableListOf<SetEntryEntity>()
    private val _flow = MutableStateFlow<List<SetEntryEntity>>(emptyList())

    override fun observeByExerciseEntry(exerciseEntryId: String): Flow<List<SetEntryEntity>> =
        _flow.map { list ->
            list.filter { it.exerciseEntryId == exerciseEntryId }
                .sortedBy { it.setNumber }
        }
    override suspend fun getById(id: String): SetEntryEntity? = store.find { it.id == id }
    override suspend fun getAllForBackup(): List<SetEntryEntity> = store.toList()
    override suspend fun getPreviousSessionSets(
        exerciseName: String,
        excludeSessionId: String,
    ): List<SetEntryEntity> = emptyList()
    override suspend fun getPreviousSessionDate(
        exerciseName: String,
        excludeSessionId: String,
    ): Long? = null
    override suspend fun insert(entity: SetEntryEntity) {
        store.removeAll { it.id == entity.id }
        store.add(entity)
        _flow.value = store.toList()
    }
    override suspend fun insertAll(entities: List<SetEntryEntity>) =
        entities.forEach { insert(it) }
    override suspend fun update(entity: SetEntryEntity) {
        store.replaceAll { if (it.id == entity.id) entity else it }
        _flow.value = store.toList()
    }
    override suspend fun delete(entity: SetEntryEntity) {
        store.remove(entity)
        _flow.value = store.toList()
    }
    override suspend fun deleteById(id: String) {
        store.removeAll { it.id == id }
        _flow.value = store.toList()
    }
    override suspend fun deleteByExerciseEntry(exerciseEntryId: String) {
        store.removeAll { it.exerciseEntryId == exerciseEntryId }
        _flow.value = store.toList()
    }
    override suspend fun deleteAll() {
        store.clear()
        _flow.value = emptyList()
    }
    override fun observePrsByRepCount(exerciseName: String): Flow<List<ExercisePr>> =
        flowOf(emptyList())
}
