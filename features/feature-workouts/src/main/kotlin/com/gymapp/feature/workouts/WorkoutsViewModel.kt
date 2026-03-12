package com.gymapp.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = workoutRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createWorkout(onCreated: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val session = workoutRepository.create(date = System.currentTimeMillis())
            onCreated(session.id)
        }
    }

    fun createFromTemplate(sourceSessionId: String, onCreated: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            val session = workoutRepository.create(date = System.currentTimeMillis())
            workoutRepository.copyExercisesFromSession(
                sourceSessionId = sourceSessionId,
                destSessionId = session.id,
            )
            onCreated(session.id)
        }
    }
}
