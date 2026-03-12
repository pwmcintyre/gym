package com.gymapp.feature.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val exercises: StateFlow<List<ExerciseEntry>> =
        workoutRepository.observeExercises(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addExercise(label: String, name: String, targetSets: Int?, targetReps: Int?) {
        viewModelScope.launch {
            workoutRepository.addExercise(
                sessionId = sessionId,
                label = label,
                exerciseName = name,
                targetSets = targetSets,
                targetReps = targetReps,
            )
        }
    }

    fun addSet(exerciseId: String, setNumber: Int, weight: Float?, reps: Int?) {
        viewModelScope.launch {
            setRepository.addSet(
                exerciseEntryId = exerciseId,
                setNumber = setNumber,
                repsPerformed = reps,
                weight = weight,
            )
        }
    }

    fun updateSet(set: SetEntry) {
        viewModelScope.launch { setRepository.update(set) }
    }

    fun deleteSet(id: String) {
        viewModelScope.launch { setRepository.delete(id) }
    }

    fun observeSets(exerciseId: String): StateFlow<List<SetEntry>> =
        setRepository.observeSets(exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
