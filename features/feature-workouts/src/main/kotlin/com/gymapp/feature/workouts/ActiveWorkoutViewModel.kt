package com.gymapp.feature.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.UserSettings
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
    private val userSettings: UserSettings,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val exercises: StateFlow<List<ExerciseEntry>> =
        workoutRepository.observeExercises(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bodyWeightKg: StateFlow<Float?> =
        userSettings.bodyWeightKg
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val knownExerciseNames: StateFlow<List<String>> =
        workoutRepository.observeDistinctExerciseNames()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastPerformance = MutableStateFlow<Map<String, List<SetEntry>>>(emptyMap())
    val lastPerformance: StateFlow<Map<String, List<SetEntry>>> = _lastPerformance.asStateFlow()

    init {
        viewModelScope.launch {
            exercises.collect { exerciseList ->
                val current = _lastPerformance.value.toMutableMap()
                exerciseList.forEach { exercise ->
                    if (exercise.exerciseName !in current) {
                        val prev = setRepository.getPreviousSessionSets(
                            exerciseName = exercise.exerciseName,
                            excludeSessionId = sessionId,
                        )
                        if (prev.isNotEmpty()) current[exercise.exerciseName] = prev
                    }
                }
                _lastPerformance.value = current
            }
        }
    }

    fun renameExercise(exercise: ExerciseEntry, newName: String) {
        viewModelScope.launch {
            workoutRepository.updateExercise(exercise.copy(exerciseName = newName.trim()))
        }
    }

    fun removeExercise(exerciseId: String) {
        viewModelScope.launch {
            workoutRepository.deleteExercise(exerciseId)
        }
    }

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

    fun addSet(
        exerciseId: String,
        setNumber: Int,
        weight: Float?,
        reps: Int?,
        weightMode: WeightMode = WeightMode.BARBELL,
    ) {
        viewModelScope.launch {
            setRepository.addSet(
                exerciseEntryId = exerciseId,
                setNumber = setNumber,
                repsPerformed = reps,
                weight = weight,
                weightMode = weightMode,
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
