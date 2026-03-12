package com.gymapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymapp.core.database.converter.Converters
import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.ExerciseTemplateDao
import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.database.entity.ExerciseTemplateEntity
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.database.entity.WorkoutSessionEntity

/**
 * The single Room database for the app.
 *
 * When making schema changes:
 *  1. Increment [version].
 *  2. Add a [androidx.room.migration.Migration] object in the `migrations` package.
 *  3. Register the migration in [di.DatabaseModule].
 */
@Database(
    entities = [
        ExerciseTemplateEntity::class,
        WorkoutSessionEntity::class,
        ExerciseEntryEntity::class,
        SetEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {

    abstract fun exerciseTemplateDao(): ExerciseTemplateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseEntryDao(): ExerciseEntryDao
    abstract fun setEntryDao(): SetEntryDao

    companion object {
        const val DATABASE_NAME = "gym.db"
    }
}
