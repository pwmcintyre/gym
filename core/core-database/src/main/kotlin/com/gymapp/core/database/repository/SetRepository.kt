package com.gymapp.core.database.repository

import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.model.ExercisePr
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetRepository @Inject constructor(
    private val setDao: SetEntryDao,
) {
    fun observeSets(exerciseEntryId: String): Flow<List<SetEntry>> =
        setDao.observeByExerciseEntry(exerciseEntryId).map { list -> list.map { it.toModel() } }

    suspend fun addSet(
        exerciseEntryId: String,
        setNumber: Int,
        repsPerformed: Int? = null,
        weight: Float? = null,
        notes: String? = null,
        weightMode: WeightMode = WeightMode.BARBELL,
    ): SetEntry {
        val set = SetEntry(
            id = UUID.randomUUID().toString(),
            exerciseEntryId = exerciseEntryId,
            setNumber = setNumber,
            repsPerformed = repsPerformed,
            weight = weight,
            notes = notes,
            weightMode = weightMode,
        )
        setDao.insert(set.toEntity())
        return set
    }

    suspend fun getPreviousSessionSets(exerciseName: String, excludeSessionId: String): List<SetEntry> =
        setDao.getPreviousSessionSets(exerciseName, excludeSessionId).map { it.toModel() }

    fun observePrsByRepCount(exerciseName: String): Flow<List<ExercisePr>> =
        setDao.observePrsByRepCount(exerciseName)

    suspend fun update(set: SetEntry) =
        setDao.update(set.toEntity())

    suspend fun delete(id: String) =
        setDao.deleteById(id)
}

private fun SetEntryEntity.toModel() =
    SetEntry(id, exerciseEntryId, setNumber, repsPerformed, weight, notes, weightMode)

private fun SetEntry.toEntity() =
    SetEntryEntity(id, exerciseEntryId, setNumber, repsPerformed, weight, notes, weightMode)
