package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.SetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {

    @Query("SELECT * FROM set_entries WHERE exercise_entry_id = :exerciseEntryId ORDER BY set_number ASC")
    fun observeByExerciseEntry(exerciseEntryId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getById(id: String): SetEntryEntity?

    @Query("SELECT * FROM set_entries ORDER BY exercise_entry_id ASC, set_number ASC, id ASC")
    suspend fun getAllForBackup(): List<SetEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SetEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SetEntryEntity>)

    @Update
    suspend fun update(entity: SetEntryEntity)

    @Delete
    suspend fun delete(entity: SetEntryEntity)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM set_entries WHERE exercise_entry_id = :exerciseEntryId")
    suspend fun deleteByExerciseEntry(exerciseEntryId: String)

    @Query("DELETE FROM set_entries")
    suspend fun deleteAll()
}
