package com.gymapp.feature.history

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseWorkoutProgression
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WorkoutComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    workoutRepository: WorkoutRepository,
) : ViewModel() {

    val workoutName: String = Uri.decode(checkNotNull(savedStateHandle["workoutName"]))

    /**
     * Per-exercise best weight per session, oldest → newest.
     * Grouped externally by the UI as `Map<exerciseName, List<ExerciseWorkoutProgression>>`.
     */
    val exerciseProgressions: StateFlow<List<ExerciseWorkoutProgression>> =
        workoutRepository.observeExerciseProgressions(workoutName)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
