package com.gymapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.core.database.entity.ExerciseTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseTemplateDao {

    @Query("SELECT * FROM exercise_templates ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseTemplateEntity>>

    @Query("SELECT * FROM exercise_templates ORDER BY name ASC, id ASC")
    suspend fun getAllForBackup(): List<ExerciseTemplateEntity>

    @Query("SELECT * FROM exercise_templates WHERE id = :id")
    suspend fun getById(id: String): ExerciseTemplateEntity?

    @Query("SELECT * FROM exercise_templates WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ExerciseTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseTemplateEntity>)

    @Update
    suspend fun update(entity: ExerciseTemplateEntity)

    @Delete
    suspend fun delete(entity: ExerciseTemplateEntity)

    @Query("DELETE FROM exercise_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exercise_templates")
    suspend fun deleteAll()
}
