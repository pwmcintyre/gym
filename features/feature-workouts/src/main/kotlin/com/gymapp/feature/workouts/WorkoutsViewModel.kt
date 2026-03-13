package com.gymapp.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.SessionSummary
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val aiAssistant: AiAssistant,
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> = workoutRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progressSummaries: StateFlow<List<ExerciseProgressSummary>> =
        workoutRepository.observeProgressSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sessionSummaries: StateFlow<Map<String, SessionSummary>> =
        workoutRepository.observeSessionSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _suggestedNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val suggestedNames: StateFlow<Map<String, String>> = _suggestedNames.asStateFlow()

    init {
        viewModelScope.launch {
            sessions.collect { sessionList ->
                supervisorScope {
                    sessionList
                        .filter { it.notes.isNullOrBlank() && !_suggestedNames.value.containsKey(it.id) }
                        .forEach { session ->
                            launch {
                                runCatching {
                                    val names = workoutRepository.getExerciseNames(session.id)
                                    if (names.isEmpty()) return@runCatching
                                    val result = aiAssistant.chat(
                                        model = "gpt-4o-mini",
                                        systemPrompt = "You are a workout classifier. Given a list of exercises, respond with a single short label (1-3 words, e.g. 'Legs', 'Push', 'Pull', 'Back', 'Chest', 'Full Body'). Respond with ONLY the label.",
                                        messages = listOf(
                                            ChatMessage(
                                                ChatMessage.Role.USER,
                                                "Exercises: ${names.joinToString(", ")}",
                                            )
                                        ),
                                    )
                                    result.getOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { label ->
                                        _suggestedNames.value = _suggestedNames.value + (session.id to label)
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

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
