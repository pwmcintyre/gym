package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.ExerciseEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEntryDao {

    @Query("SELECT * FROM exercise_entries WHERE workout_session_id = :sessionId ORDER BY label ASC")
    fun observeBySession(sessionId: String): Flow<List<ExerciseEntryEntity>>

    @Query("SELECT * FROM exercise_entries WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseEntryEntity>)

    @Update
    suspend fun update(entity: ExerciseEntryEntity)

    @Delete
    suspend fun delete(entity: ExerciseEntryEntity)

    @Query("DELETE FROM exercise_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exercise_entries WHERE workout_session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
