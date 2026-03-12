package com.gymapp.core.database.repository

import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.model.SetEntry
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
    ): SetEntry {
        val set = SetEntry(
            id = UUID.randomUUID().toString(),
            exerciseEntryId = exerciseEntryId,
            setNumber = setNumber,
            repsPerformed = repsPerformed,
            weight = weight,
            notes = notes,
        )
        setDao.insert(set.toEntity())
        return set
    }

    suspend fun update(set: SetEntry) =
        setDao.update(set.toEntity())

    suspend fun delete(id: String) =
        setDao.deleteById(id)
}

private fun SetEntryEntity.toModel() =
    SetEntry(id, exerciseEntryId, setNumber, repsPerformed, weight, notes)

private fun SetEntry.toEntity() =
    SetEntryEntity(id, exerciseEntryId, setNumber, repsPerformed, weight, notes)
