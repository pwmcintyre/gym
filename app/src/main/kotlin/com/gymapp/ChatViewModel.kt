package com.gymapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import com.gymapp.core.ai.CoachSuggestionStore
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
import org.json.JSONObject

data class UiChatMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val action: UiChatAction? = null,
)

data class UiChatAction(
    val label: String,
    val prompt: String,
)

data class ChatScreenContext(
    val screenName: String,
    val entityType: String? = null,
    val entityValue: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiAssistant: AiAssistant,
    private val coachSuggestionStore: CoachSuggestionStore,
    private val userSettings: UserSettings,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) : ViewModel() {

    private val _messages = mutableStateListOf<UiChatMessage>()
    val messages: List<UiChatMessage> get() = _messages

    var isLoading by mutableStateOf(false)
        private set

    private var screenContext by mutableStateOf(ChatScreenContext(screenName = "workouts"))
    private var hasTriggeredColdLaunchMessage = false

    fun updateScreenContext(context: ChatScreenContext) {
        screenContext = context
    }

    fun shouldTriggerColdLaunchMessage(): Boolean = !hasTriggeredColdLaunchMessage

    fun triggerColdLaunchMessage() {
        if (hasTriggeredColdLaunchMessage) return
        hasTriggeredColdLaunchMessage = true
        if (_messages.isNotEmpty()) return

        viewModelScope.launch {
            val sessions = workoutRepository.observeAll().first()
            val recentSessions = sessions.filter { it.date >= System.currentTimeMillis() - COLD_LAUNCH_WINDOW_MS }
            if (recentSessions.isEmpty()) {
                _messages.add(
                    UiChatMessage(
                        content = "Welcome! Log your first workout, or ask me anything.",
                        isUser = false,
                    ),
                )
                return@launch
            }

            isLoading = true
            val recentSummary = buildRecentSummary(recentSessions, workoutRepository)
            val systemPrompt = buildSystemPrompt(
                context = screenContext,
                trainingConstraints = userSettings.trainingConstraints.first(),
                workoutRepository = workoutRepository,
                setRepository = setRepository,
            ) + """

                Write a proactive opening message for app launch as valid JSON only:
                {
                  "review": "One honest but encouraging sentence about the recent training history.",
                  "next_workout_suggestion": "One sentence suggesting the next workout.",
                  "button_label": "Short CTA label",
                  "button_prompt": "A concise workout-planning prompt to expand into a full workout."
                }

                Rules:
                - `review` must be exactly one sentence.
                - `next_workout_suggestion` must be exactly one sentence.
                - Keep both direct and specific.
                - `button_label` should be 2-4 words.
                - `button_prompt` should give enough detail to build the workout plan.
            """.trimIndent()
            val result = aiAssistant.chat(
                systemPrompt = systemPrompt,
                messages = listOf(
                    ChatMessage(
                        role = ChatMessage.Role.USER,
                        content = "Use this recent training summary for your opener: $recentSummary",
                    )
                ),
            )
            isLoading = false
            result.fold(
                onSuccess = { reply ->
                    _messages.add(parseColdLaunchMessage(reply, recentSessions))
                },
                onFailure = {
                    _messages.add(
                        UiChatMessage(
                            content = fallbackOpeningMessage(recentSessions),
                            isUser = false,
                        ),
                    )
                },
            )
        }
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

    suspend fun createWorkoutFromCoachSuggestion(prompt: String): String {
        val session = workoutRepository.create(date = System.currentTimeMillis())
        coachSuggestionStore.set(session.id, prompt)
        return session.id
    }

    companion object {
        private const val COLD_LAUNCH_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
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

private fun parseColdLaunchMessage(reply: String, recentSessions: List<WorkoutSession>): UiChatMessage {
    val stripped = reply.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    return runCatching {
        val parsed = JSONObject(stripped)
        val review = parsed.optString("review").trim()
        val suggestion = parsed.optString("next_workout_suggestion").trim()
        val buttonLabel = parsed.optString("button_label").ifBlank { "Use workout" }
        val buttonPrompt = parsed.optString("button_prompt").trim()
        UiChatMessage(
            content = "$review $suggestion".trim(),
            isUser = false,
            action = buttonPrompt.takeIf { it.isNotBlank() }?.let { prompt ->
                UiChatAction(
                    label = buttonLabel,
                    prompt = prompt,
                )
            },
        )
    }.getOrElse {
        UiChatMessage(
            content = fallbackOpeningMessage(recentSessions),
            isUser = false,
        )
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

private suspend fun buildRecentSummary(
    sessions: List<WorkoutSession>,
    workoutRepository: WorkoutRepository,
): String {
    val movementNames = sessions.flatMap { session ->
        workoutRepository.observeExercises(session.id).first().map { it.exerciseName }
    }
        .filter { it.isNotBlank() }
    val topMovements = movementNames
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)
        .joinToString { "${it.key} (${it.value})" }

    return buildString {
        append("sessions=")
        append(sessions.size)
        if (topMovements.isNotBlank()) {
            append("; common movements=")
            append(topMovements)
        }
    }
}

private fun fallbackOpeningMessage(sessions: List<WorkoutSession>): String {
    val count = sessions.size
    val dayLabel = if (count == 1) "session" else "sessions"
    val recentDay = sessions.maxByOrNull { it.date }?.let { formatEpochDate(it.date) }
    val hook = if (count >= 3) {
        "Want me to help line up the next one?"
    } else {
        "Want to plan today's training?"
    }
    return buildString {
        append("You trained ")
        append(count)
        append(' ')
        append(dayLabel)
        append(" in the last 7 days")
        recentDay?.let {
            append(", most recently on ")
            append(it)
        }
        append(". ")
        append(hook)
    }
}
