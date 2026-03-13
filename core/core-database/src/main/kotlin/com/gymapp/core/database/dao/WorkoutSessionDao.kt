package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.WorkoutSessionEntity
import com.gymapp.core.model.WorkoutComparisonSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions ORDER BY created_at ASC, id ASC")
    suspend fun getAllForBackup(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WorkoutSessionEntity>)

    @Update
    suspend fun update(entity: WorkoutSessionEntity)

    @Delete
    suspend fun delete(entity: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()

    /**
     * Returns per-session volume totals for all sessions whose name matches [name]
     * (case-insensitive trim), ordered oldest → newest (for charting).
     */
    @Query(
        """
        SELECT
            ws.id AS sessionId,
            ws.date AS sessionDate,
            COALESCE(
                CAST(SUM(s.weight * s.reps_performed) AS REAL),
                0
            ) AS totalVolume
        FROM workout_sessions ws
        LEFT JOIN exercise_entries e ON e.workout_session_id = ws.id
        LEFT JOIN set_entries s ON s.exercise_entry_id = e.id
        WHERE LOWER(TRIM(ws.notes)) = LOWER(TRIM(:name))
        GROUP BY ws.id, ws.date
        ORDER BY ws.date ASC
        """
    )
    fun observeComparisonSessions(name: String): Flow<List<WorkoutComparisonSession>>

    /** Returns the distinct workout names (non-null session notes), most recent first. */
    @Query(
        "SELECT DISTINCT notes FROM workout_sessions WHERE notes IS NOT NULL AND TRIM(notes) != '' ORDER BY date DESC"
    )
    fun observeDistinctWorkoutNames(): Flow<List<String>>
}
