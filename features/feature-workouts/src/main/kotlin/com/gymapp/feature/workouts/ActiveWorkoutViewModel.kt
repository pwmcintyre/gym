package com.gymapp.feature.workouts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import com.gymapp.core.ai.UserSettings
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SuggestionType { DOUBLE_DOWN, FILL_GAPS, CARDIO }

sealed class RestTimerState {
    object Idle : RestTimerState()
    data class Running(val exerciseId: String, val remainingSeconds: Int, val totalSeconds: Int) : RestTimerState()
}

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
    private val userSettings: UserSettings,
    private val aiAssistant: AiAssistant,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val session: StateFlow<WorkoutSession?> =
        workoutRepository.observeById(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    private val _lastPerformanceDate = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastPerformanceDate: StateFlow<Map<String, Long>> = _lastPerformanceDate.asStateFlow()

    private val _restTimer = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()

    private var timerJob: Job? = null

    private val setFlowCache = mutableMapOf<String, StateFlow<List<SetEntry>>>()

    init {
        viewModelScope.launch {
            exercises.collect { exerciseList ->
                val current = _lastPerformance.value.toMutableMap()
                val currentDates = _lastPerformanceDate.value.toMutableMap()
                exerciseList.forEach { exercise ->
                    if (exercise.exerciseName !in current) {
                        val prev = setRepository.getPreviousSessionSets(
                            exerciseName = exercise.exerciseName,
                            excludeSessionId = sessionId,
                        )
                        if (prev.isNotEmpty()) current[exercise.exerciseName] = prev
                    }
                    if (exercise.exerciseName !in currentDates) {
                        val date = setRepository.getPreviousSessionDate(
                            exerciseName = exercise.exerciseName,
                            excludeSessionId = sessionId,
                        )
                        if (date != null) currentDates[exercise.exerciseName] = date
                    }
                }
                _lastPerformance.value = current
                _lastPerformanceDate.value = currentDates
            }
        }

        // Auto-name the session via AI after exercises settle (debounced 2s)
        viewModelScope.launch {
            exercises.collect { exs ->
                if (exs.isNotEmpty() && session.value?.notes.isNullOrBlank()) {
                    namingJob?.cancel()
                    namingJob = launch {
                        delay(2_000)
                        if (session.value?.notes.isNullOrBlank()) {
                            _isNaming.value = true
                            runCatching {
                                val names = exs.map { it.exerciseName }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                val result = aiAssistant.chat(
                                    model = "gpt-4o-mini",
                                    systemPrompt = "You are a workout classifier. Given a list of exercises, respond with a single short label (1-3 words, e.g. 'Legs', 'Push', 'Pull', 'Back', 'Chest', 'Full Body'). Respond with ONLY the label.",
                                    messages = listOf(
                                        ChatMessage(ChatMessage.Role.USER, "Exercises: ${names.joinToString(", ")}"),
                                    ),
                                )
                                result.getOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { label ->
                                    val current = workoutRepository.getById(sessionId) ?: return@let
                                    workoutRepository.update(current.copy(notes = label))
                                }
                            }
                            _isNaming.value = false
                        }
                    }
                }
            }
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val current = workoutRepository.getById(sessionId) ?: return@launch
            workoutRepository.update(current.copy(notes = notes.takeIf { it.isNotBlank() }))
        }
    }

    fun updateDate(epochMillis: Long) {
        viewModelScope.launch {
            val current = workoutRepository.getById(sessionId) ?: return@launch
            workoutRepository.update(current.copy(date = epochMillis))
        }
    }

    fun renameExercise(exercise: ExerciseEntry, newName: String) {
        viewModelScope.launch {
            workoutRepository.updateExercise(exercise.copy(exerciseName = newName.trim()))
        }
    }

    fun updateExercise(exercise: ExerciseEntry) {
        viewModelScope.launch {
            workoutRepository.updateExercise(exercise)
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
            startRestTimer(exerciseId)
        }
    }

    fun updateSet(set: SetEntry) {
        viewModelScope.launch { setRepository.update(set) }
    }

    fun deleteSet(id: String) {
        viewModelScope.launch { setRepository.delete(id) }
    }

    fun observeSets(exerciseId: String): StateFlow<List<SetEntry>> =
        setFlowCache.getOrPut(exerciseId) {
            setRepository.observeSets(exerciseId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    private val _isSuggesting = MutableStateFlow(false)
    val isSuggesting: StateFlow<Boolean> = _isSuggesting.asStateFlow()

    private val _isNaming = MutableStateFlow(false)
    val isNaming: StateFlow<Boolean> = _isNaming.asStateFlow()

    private var namingJob: Job? = null

    fun suggestWorkout(type: SuggestionType) {
        viewModelScope.launch {
            _isSuggesting.value = true
            runCatching {
                // Build context from last 5 sessions
                val recent = workoutRepository.observeAll().first().take(5)
                val contextLines = mutableListOf<String>()
                for (s in recent) {
                    val names = workoutRepository.getExerciseNames(s.id)
                    val label = s.notes?.takeIf { it.isNotBlank() } ?: "Unnamed"
                    contextLines.add("$label: ${names.joinToString(", ")}")
                }
                val context = contextLines.joinToString("\n")

                val goalPrompt = when (type) {
                    SuggestionType.DOUBLE_DOWN ->
                        "Suggest a workout that continues my recent training pattern. Push harder on the same movements or similar ones — progressive overload."
                    SuggestionType.FILL_GAPS ->
                        "Suggest a workout that complements my recent training. Focus on muscle groups I haven't trained recently to keep things balanced."
                    SuggestionType.CARDIO ->
                        "Suggest a practical conditioning/cardio workout suitable for someone who also trains with weights."
                }

                val result = aiAssistant.chat(
                    model = "gpt-4o-mini",
                    systemPrompt = """
                        You are a workout planner. Given recent workout history and a goal, suggest 3–6 exercises.
                        Respond ONLY with one exercise per line in this exact format:
                        Exercise Name | sets | reps
                        Use whole numbers for sets and reps. No extra text, no headers, no blank lines.
                    """.trimIndent(),
                    messages = listOf(
                        ChatMessage(
                            ChatMessage.Role.USER,
                            "Recent sessions:\n$context\n\nGoal: $goalPrompt",
                        ),
                    ),
                )

                val lines = result.getOrThrow().trim().lines()
                val labelPool = listOf("A1","B1","C1","D1","E1","F1","G1","H1")
                val startIdx = exercises.value.size
                lines.forEachIndexed { i, line ->
                    val parts = line.split("|").map { it.trim() }
                    if (parts.size >= 1 && parts[0].isNotBlank()) {
                        val name = parts[0]
                        val sets = parts.getOrNull(1)?.toIntOrNull()
                        val reps = parts.getOrNull(2)?.toIntOrNull()
                        val label = labelPool.getOrElse(startIdx + i) { "${startIdx + i + 1}" }
                        workoutRepository.addExercise(
                            sessionId = sessionId,
                            label = label,
                            exerciseName = name,
                            targetSets = sets,
                            targetReps = reps,
                        )
                    }
                }
            }
            _isSuggesting.value = false
        }
    }

    fun cancelRestTimer() {
        timerJob?.cancel()
        _restTimer.value = RestTimerState.Idle
    }

    private fun startRestTimer(exerciseId: String) {
        timerJob?.cancel()
        viewModelScope.launch {
            val totalSeconds = userSettings.restTimerSeconds.first()
            _restTimer.value = RestTimerState.Running(exerciseId, totalSeconds, totalSeconds)
            for (remaining in (totalSeconds - 1) downTo 0) {
                delay(1_000)
                if (_restTimer.value is RestTimerState.Idle) return@launch
                _restTimer.value = RestTimerState.Running(exerciseId, remaining, totalSeconds)
            }
        }.also { timerJob = it }
    }
}
