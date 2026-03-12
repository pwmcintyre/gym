package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.ExerciseSessionProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseEntryDao {

    @Query("SELECT * FROM exercise_entries WHERE workout_session_id = :sessionId ORDER BY label ASC")
    fun observeBySession(sessionId: String): Flow<List<ExerciseEntryEntity>>

    @Query("SELECT * FROM exercise_entries WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntryEntity?

    @Query("SELECT * FROM exercise_entries ORDER BY workout_session_id ASC, label ASC, id ASC")
    suspend fun getAllForBackup(): List<ExerciseEntryEntity>

    @Query(
        """
        SELECT
            e.exercise_name AS exerciseName,
            COUNT(DISTINCT e.workout_session_id) AS sessionCount,
            MAX(ws.date) AS lastPerformed,
            MAX(s.weight) AS bestWeight,
            CAST(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps_performed, 0)) AS REAL) AS totalVolume
        FROM exercise_entries e
        INNER JOIN workout_sessions ws ON ws.id = e.workout_session_id
        LEFT JOIN set_entries s ON s.exercise_entry_id = e.id
        WHERE TRIM(e.exercise_name) != ''
        GROUP BY e.exercise_name
        ORDER BY lastPerformed DESC, sessionCount DESC, exerciseName COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeProgressSummaries(limit: Int): Flow<List<ExerciseProgressSummary>>

    @Query(
        """
        SELECT
            ws.id AS sessionId,
            ws.date AS sessionDate,
            COUNT(s.id) AS setCount,
            MAX(s.weight) AS bestWeight,
            CAST(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps_performed, 0)) AS REAL) AS totalVolume
        FROM exercise_entries e
        INNER JOIN workout_sessions ws ON ws.id = e.workout_session_id
        LEFT JOIN set_entries s ON s.exercise_entry_id = e.id
        WHERE e.exercise_name = :exerciseName
        GROUP BY ws.id, ws.date
        ORDER BY ws.date DESC
        """
    )
    fun observeProgressSessions(exerciseName: String): Flow<List<ExerciseSessionProgress>>

    @Query("SELECT DISTINCT exercise_name FROM exercise_entries WHERE TRIM(exercise_name) != '' ORDER BY exercise_name COLLATE NOCASE ASC")
    fun observeDistinctNames(): Flow<List<String>>

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

    @Query("DELETE FROM exercise_entries")
    suspend fun deleteAll()
}
