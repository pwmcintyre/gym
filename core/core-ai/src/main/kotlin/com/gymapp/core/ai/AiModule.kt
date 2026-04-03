package com.gymapp.core.ai

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    /**
     * Route all [WorkoutCardParser] injection to [SmartWorkoutCardParser],
     * which dispatches to OpenAI or local Gemma based on API key availability.
     */
    @Binds
    @Singleton
    abstract fun bindWorkoutCardParser(impl: SmartWorkoutCardParser): WorkoutCardParser
}
