package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.model.ExercisePr
import kotlinx.coroutines.flow.Flow

@Dao
interface SetEntryDao {

    @Query("SELECT * FROM set_entries WHERE exercise_entry_id = :exerciseEntryId ORDER BY set_number ASC")
    fun observeByExerciseEntry(exerciseEntryId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getById(id: String): SetEntryEntity?

    @Query("SELECT * FROM set_entries ORDER BY exercise_entry_id ASC, set_number ASC, id ASC")
    suspend fun getAllForBackup(): List<SetEntryEntity>

    /**
     * Returns all sets from the most recent prior session where the exercise name matches,
     * excluding the given session (i.e. the current one).
     */
    @Query(
        """
        SELECT s.*
        FROM set_entries s
        INNER JOIN exercise_entries e ON s.exercise_entry_id = e.id
        INNER JOIN workout_sessions ws ON e.workout_session_id = ws.id
        WHERE e.exercise_name = :exerciseName
          AND e.workout_session_id != :excludeSessionId
          AND ws.date = (
              SELECT MAX(ws2.date)
              FROM exercise_entries e2
              INNER JOIN workout_sessions ws2 ON e2.workout_session_id = ws2.id
              WHERE e2.exercise_name = :exerciseName
                AND e2.workout_session_id != :excludeSessionId
          )
        ORDER BY s.set_number ASC
        """
    )
    suspend fun getPreviousSessionSets(exerciseName: String, excludeSessionId: String): List<SetEntryEntity>

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

    /**
     * Returns the all-time best weight per rep count for the given exercise name,
     * ordered by rep count ascending (1RM first, then 2RM, 3RM …).
     * Only counts sets with a positive weight and at least 1 rep.
     */
    @Query(
        """
        SELECT
            s.reps_performed AS reps,
            MAX(s.weight)    AS weight
        FROM set_entries s
        INNER JOIN exercise_entries e ON s.exercise_entry_id = e.id
        WHERE e.exercise_name = :exerciseName
          AND s.weight > 0
          AND s.reps_performed > 0
        GROUP BY s.reps_performed
        ORDER BY s.reps_performed ASC
        """
    )
    fun observePrsByRepCount(exerciseName: String): Flow<List<ExercisePr>>
}
