package com.gymapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.gymapp.core.database.GymDatabase
import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.ExerciseTemplateDao
import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGymDatabase(
        @ApplicationContext context: Context,
    ): GymDatabase = Room.databaseBuilder(
        context,
        GymDatabase::class.java,
        GymDatabase.DATABASE_NAME,
    )
        // POC default: prefer launching with a clean local database over crashing
        // when the on-device schema diverges from the checked-in Room schema.
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        // TODO: add named Migration objects here as the schema evolves, e.g.:
        //   .addMigrations(MIGRATION_1_2)
        .build()

    @Provides
    fun provideExerciseTemplateDao(db: GymDatabase): ExerciseTemplateDao =
        db.exerciseTemplateDao()

    @Provides
    fun provideWorkoutSessionDao(db: GymDatabase): WorkoutSessionDao =
        db.workoutSessionDao()

    @Provides
    fun provideExerciseEntryDao(db: GymDatabase): ExerciseEntryDao =
        db.exerciseEntryDao()

    @Provides
    fun provideSetEntryDao(db: GymDatabase): SetEntryDao =
        db.setEntryDao()
}
