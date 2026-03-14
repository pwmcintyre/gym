package com.gymapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import com.gymapp.core.ai.UserSettings
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class UiChatMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
)

data class ChatScreenContext(
    val screenName: String,
    val entityType: String? = null,
    val entityValue: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiAssistant: AiAssistant,
    private val userSettings: UserSettings,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) : ViewModel() {

    private val _messages = mutableStateListOf<UiChatMessage>()
    val messages: List<UiChatMessage> get() = _messages

    var isLoading by mutableStateOf(false)
        private set

    private var screenContext by mutableStateOf(ChatScreenContext(screenName = "workouts"))

    fun updateScreenContext(context: ChatScreenContext) {
        screenContext = context
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _messages.add(UiChatMessage(trimmed, isUser = true))
        isLoading = true

        viewModelScope.launch {
            val history = _messages
                .filter { !it.isError }
                .map {
                    ChatMessage(
                        role = if (it.isUser) ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT,
                        content = it.content,
                    )
                }

            val systemPrompt = buildSystemPrompt(
                context = screenContext,
                trainingConstraints = userSettings.trainingConstraints.first(),
                workoutRepository = workoutRepository,
                setRepository = setRepository,
            )
            val result = aiAssistant.chat(systemPrompt, history)
            isLoading = false

            result.fold(
                onSuccess = { reply ->
                    _messages.add(UiChatMessage(reply, isUser = false))
                },
                onFailure = {
                    _messages.add(
                        UiChatMessage(
                            content = "Couldn't reach the AI, try again",
                            isUser = false,
                            isError = true,
                        ),
                    )
                },
            )
        }
    }

    companion object {
        private const val RECENT_HISTORY_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
        private const val COACH_PROMPT = """
            You are a personal training coach. Be present, concise, and direct.
            No filler, no disclaimers, no "As an AI..." hedging.
            Talk like a coach, not a chatbot. Get to the point.
        """

        internal suspend fun buildSystemPrompt(
            context: ChatScreenContext,
            trainingConstraints: String,
            workoutRepository: WorkoutRepository,
            setRepository: SetRepository,
            nowMillis: Long = System.currentTimeMillis(),
        ): String {
            val sessions = workoutRepository.observeAll().first()
            val movementNames = workoutRepository.observeDistinctExerciseNames().first()
            val cutoff = nowMillis - RECENT_HISTORY_WINDOW_MS
            val recentSessions = sessions.filter { it.date >= cutoff }
            val recentPayload = buildSessionPayload(recentSessions, workoutRepository, setRepository)
            val movementHistoryPayload = context.entityValue
                ?.takeIf { context.entityType == "movementName" && it.isNotBlank() }
                ?.let { movementName ->
                    buildMovementHistoryPayload(movementName, sessions, workoutRepository, setRepository)
                }
            val constraintsSection = trainingConstraints
                .trim()
                .takeIf { it.isNotBlank() }
                ?.let {
                    """
                    User training constraints (always respect these):
                    $it
                    """.trimIndent()
                }

            return """
                ${COACH_PROMPT.trimIndent()}

                Current app context:
                ${context.toPromptJson()}

                ${constraintsSection ?: ""}

                Data schema:
                - Session: one workout day.
                - Movement: one logged movement entry inside a session.
                - Set: one performed set under a movement, including reps, weight, and weight mode.
                - Sessions contain movements. Movements contain sets.
                - `targetModifier` describes prescribed rep modifiers such as MAX.
                - `weightMode` is `BARBELL`, `BODYWEIGHT`, or `BANDED`.

                Allowed write actions you may propose:
                - `create_session` for today.
                - `add_set` with movement name, weight, reps, optional modifier, and session date.
                - Do not pretend a write already happened unless the app confirms it.

                Known movements:
                ${movementNames.map(::jsonString).toPromptJsonArray().value}

                Recent sessions from the last 14 days:
                ${recentPayload.toPromptJsonArray().value}

                ${movementHistorySection(movementHistoryPayload)}

                Use the app context and training data naturally. Do not announce hidden context or say that you can see the screen.
            """.trimIndent()
        }
    }
}

private suspend fun buildSessionPayload(
    sessions: List<WorkoutSession>,
    workoutRepository: WorkoutRepository,
    setRepository: SetRepository,
): List<String> = sessions.sortedByDescending { it.date }.map { session ->
    session.toPromptJson(
        exercises = workoutRepository.observeExercises(session.id).first(),
        setRepository = setRepository,
    )
}

private suspend fun buildMovementHistoryPayload(
    movementName: String,
    sessions: List<WorkoutSession>,
    workoutRepository: WorkoutRepository,
    setRepository: SetRepository,
): List<String> = sessions.sortedByDescending { it.date }.mapNotNull { session ->
    val matchingExercises = workoutRepository.observeExercises(session.id)
        .first()
        .filter { it.exerciseName == movementName }
    if (matchingExercises.isEmpty()) {
        null
    } else {
        session.toPromptJson(matchingExercises, setRepository)
    }
}

private suspend fun WorkoutSession.toPromptJson(
    exercises: List<ExerciseEntry>,
    setRepository: SetRepository,
): String {
    val movementPayload = exercises.map { exercise ->
        exercise.toPromptJson(setRepository.observeSets(exercise.id).first())
    }

    return jsonObject(
        "sessionId" to id,
        "date" to formatEpochDate(date),
        "workoutName" to notes,
        "movements" to movementPayload.toPromptJsonArray(),
    )
}

private fun ExerciseEntry.toPromptJson(sets: List<SetEntry>): String = jsonObject(
    "label" to label,
    "movementName" to exerciseName,
    "targetSets" to targetSets,
    "targetReps" to targetReps,
    "targetModifier" to targetModifier.name.takeIf { targetModifier != RepModifier.NONE },
    "notes" to notes,
    "modifierTags" to modifierTags.map(::jsonString).toPromptJsonArray(),
    "sets" to sets.sortedBy { it.setNumber }.map { it.toPromptJson() }.toPromptJsonArray(),
)

private fun SetEntry.toPromptJson(): String = jsonObject(
    "setNumber" to setNumber,
    "reps" to repsPerformed,
    "weightKg" to weight,
    "weightMode" to weightMode.name,
    "notes" to notes,
)

private fun movementHistorySection(payload: List<String>?): String =
    if (payload == null) {
        "Specific movement history in context:\n[]"
    } else {
        "Specific movement history in context:\n${payload.toPromptJsonArray().value}"
    }

private fun ChatScreenContext.toPromptJson(): String {
    return jsonObject(
        "screenName" to screenName,
        "entityType" to entityType,
        "entityValue" to entityValue,
    )
}

private fun String.escapeJson(): String = buildString(length + 8) {
    for (char in this@escapeJson) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}

@JvmInline
private value class RawJson(val value: String)

private fun List<String>.toPromptJsonArray(): RawJson =
    RawJson(joinToString(prefix = "[", postfix = "]"))

private fun jsonObject(vararg fields: Pair<String, Any?>): String {
    val values = fields.mapNotNull { (key, value) ->
        value?.let { """"${key.escapeJson()}":${it.toJsonValue()}""" }
    }
    return "{${values.joinToString(",")}}"
}

private fun Any.toJsonValue(): String = when (this) {
    is RawJson -> value
    is String -> jsonString(this)
    is Int, is Long, is Float, is Double, is Boolean -> toString()
    is List<*> -> joinToString(prefix = "[", postfix = "]") { item ->
        item?.toJsonValue() ?: "null"
    }
    else -> """"${toString().escapeJson()}""""
}

private fun jsonString(value: String): String = """"${value.escapeJson()}""""

private fun formatEpochDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
