package com.gymapp.feature.history

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExercisePr
import com.gymapp.core.model.ExerciseSessionProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    workoutRepository: WorkoutRepository,
    setRepository: SetRepository,
) : ViewModel() {

    val exerciseName: String = savedStateHandle.get<String>("exerciseName")
        ?.let(Uri::decode)
        ?.takeIf { it.isNotBlank() }
        ?: "Progress"

    val sessionProgress: StateFlow<List<ExerciseSessionProgress>> =
        workoutRepository.observeProgressSessions(exerciseName)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All-time best weight keyed by rep count, ordered by reps ASC. */
    val prs: StateFlow<List<ExercisePr>> =
        setRepository.observePrsByRepCount(exerciseName)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
