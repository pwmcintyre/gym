package com.gymapp.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<WorkoutSession?> =
        workoutRepository.observeById(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<ExerciseEntry>> =
        workoutRepository.observeExercises(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val setFlowCache = mutableMapOf<String, StateFlow<List<SetEntry>>>()

    fun observeSets(exerciseId: String): StateFlow<List<SetEntry>> =
        setFlowCache.getOrPut(exerciseId) {
            setRepository.observeSets(exerciseId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.delete(sessionId)
            onDeleted()
        }
    }
}
