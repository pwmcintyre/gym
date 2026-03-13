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
import com.gymapp.core.model.ExerciseWorkoutProgression
import com.gymapp.core.model.SessionSummary
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
            CAST(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps_performed, 0)) AS REAL) AS totalVolume,
            COALESCE(SUM(CASE WHEN s.weight_mode = 'BODYWEIGHT' THEN 1 ELSE 0 END), 0) AS bodyweightSetCount
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
            MAX(s.reps_performed) AS bestReps,
            CAST(SUM(COALESCE(s.weight, 0) * COALESCE(s.reps_performed, 0)) AS REAL) AS totalVolume,
            COALESCE(SUM(CASE WHEN s.weight_mode = 'BODYWEIGHT' THEN 1 ELSE 0 END), 0) AS bodyweightSetCount
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

    @Query(
        """
        SELECT
            e.workout_session_id AS sessionId,
            COUNT(DISTINCT e.id) AS exerciseCount,
            COUNT(s.id) AS setCount,
            (
                SELECT COALESCE(SUM(e2.target_sets), 0)
                FROM exercise_entries e2
                WHERE e2.workout_session_id = e.workout_session_id
            ) AS targetSetCount
        FROM exercise_entries e
        LEFT JOIN set_entries s ON s.exercise_entry_id = e.id
        GROUP BY e.workout_session_id
        """
    )
    fun observeSessionSummaries(): Flow<List<SessionSummary>>

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

    /**
     * Returns the best weight per exercise per session for all sessions with the given workout name.
     * Ordered by exercise name then date ascending (for charting a trend per exercise).
     */
    @Query(
        """
        SELECT
            e.exercise_name AS exerciseName,
            ws.id AS sessionId,
            ws.date AS sessionDate,
            MAX(s.weight) AS bestWeight
        FROM workout_sessions ws
        INNER JOIN exercise_entries e ON e.workout_session_id = ws.id
        INNER JOIN set_entries s ON s.exercise_entry_id = e.id
        WHERE LOWER(TRIM(ws.notes)) = LOWER(TRIM(:workoutName))
          AND s.weight IS NOT NULL AND s.weight > 0
        GROUP BY e.exercise_name, ws.id
        ORDER BY e.exercise_name COLLATE NOCASE ASC, ws.date ASC
        """
    )
    fun observeExerciseProgressions(workoutName: String): Flow<List<ExerciseWorkoutProgression>>

    /** Renames every occurrence of [oldName] across all sessions. */
    @Query("UPDATE exercise_entries SET exercise_name = :newName WHERE exercise_name = :oldName")
    suspend fun renameGlobally(oldName: String, newName: String)

    @Query("SELECT DISTINCT exercise_name FROM exercise_entries WHERE workout_session_id = :sessionId AND TRIM(exercise_name) != '' ORDER BY label ASC")
    suspend fun getExerciseNamesForSession(sessionId: String): List<String>

    @Query("DELETE FROM exercise_entries")
    suspend fun deleteAll()
}
