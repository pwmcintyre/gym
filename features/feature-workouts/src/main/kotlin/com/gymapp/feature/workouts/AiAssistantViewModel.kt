package com.gymapp.feature.workouts

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiAssistant
import com.gymapp.core.ai.ChatMessage
import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.ExerciseProgressSummary
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WeightMode
import com.gymapp.core.model.formatDate
import com.gymapp.core.model.formatWeight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Represents a single chat bubble in the AI assistant conversation.
 */
data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
) {
    enum class Role { USER, ASSISTANT, ERROR }
}

/** Describes a pending auto-fill operation the user can accept or dismiss. */
data class AutoFillProposal(
    val fills: List<ExerciseFill>,
    val summary: String,
)

data class ExerciseFill(
    val exerciseName: String,
    val sets: List<SetFill>,
)

data class SetFill(val reps: Int?, val weight: Float?)

sealed class MicState {
    object Idle : MicState()
    object Listening : MicState()
    data class Error(val message: String) : MicState()
}

/**
 * ViewModel backing the AI assistant bottom sheet.
 *
 * Designed to be created and held by the *host* screen ViewModel's scope or by the Compose
 * screen directly via [hiltViewModel]. The [sessionId] and [contextMode] are set once after
 * creation via [init].
 */
@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    application: Application,
    private val aiAssistant: AiAssistant,
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) : AndroidViewModel(application) {

    // ── Public state ────────────────────────────────────────────────────────────

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _micState = MutableStateFlow<MicState>(MicState.Idle)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _autoFillProposal = MutableStateFlow<AutoFillProposal?>(null)
    val autoFillProposal: StateFlow<AutoFillProposal?> = _autoFillProposal.asStateFlow()

    // ── Internal state ───────────────────────────────────────────────────────────

    /** Conversation history sent to the API (does not include the system prompt). */
    private val conversationHistory = mutableListOf<ChatMessage>()

    /** Set by the host when opening the sheet. */
    private var sessionId: String? = null
    private var isWorkoutMode: Boolean = false // true = in-workout auto-fill, false = history Q&A

    private var speechRecognizer: SpeechRecognizer? = null

    // ── Public API ───────────────────────────────────────────────────────────────

    /**
     * Called by the host screen to attach session context before the sheet is shown.
     * Safe to call multiple times; only takes effect on first call per instance.
     */
    fun configure(sessionId: String?, workoutMode: Boolean) {
        this.sessionId = sessionId
        this.isWorkoutMode = workoutMode
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = AssistantMessage(role = AssistantMessage.Role.USER, text = text)
        _messages.value = _messages.value + userMsg
        conversationHistory.add(ChatMessage(role = ChatMessage.Role.USER, content = text))

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val systemPrompt = buildSystemPrompt()
                val result = aiAssistant.chat(systemPrompt, conversationHistory.toList())
                result.onSuccess { reply ->
                    conversationHistory.add(ChatMessage(role = ChatMessage.Role.ASSISTANT, content = reply))
                    // Try to parse as structured JSON; fall back to plain text answer
                    val proposal = tryParseAutoFill(reply)
                    if (proposal != null && isWorkoutMode) {
                        _autoFillProposal.value = proposal
                        val displayText = proposal.summary.ifBlank { "I've prepared a set of fills for you — tap Apply to confirm." }
                        _messages.value = _messages.value + AssistantMessage(
                            role = AssistantMessage.Role.ASSISTANT, text = displayText
                        )
                    } else {
                        val displayText = tryExtractAnswer(reply)
                        _messages.value = _messages.value + AssistantMessage(
                            role = AssistantMessage.Role.ASSISTANT, text = displayText
                        )
                    }
                }.onFailure { e ->
                    val errMsg = e.message ?: "Unknown error"
                    _messages.value = _messages.value + AssistantMessage(
                        role = AssistantMessage.Role.ERROR, text = errMsg
                    )
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearProposal() {
        _autoFillProposal.value = null
    }

    /**
     * Applies the current [AutoFillProposal] to the active workout session.
     * Matches exercises by name (case-insensitive). Adds new sets to each matched exercise.
     */
    fun applyAutoFill() {
        val proposal = _autoFillProposal.value ?: return
        val sid = sessionId ?: return
        viewModelScope.launch {
            val exercises = workoutRepository.observeExercises(sid).first()
            proposal.fills.forEach { fill ->
                val exercise = exercises.firstOrNull {
                    it.exerciseName.equals(fill.exerciseName, ignoreCase = true)
                } ?: return@forEach

                // Get current set count so new sets are numbered correctly
                val existingSets = setRepository.observeSets(exercise.id).first()
                var nextSetNumber = existingSets.size + 1

                fill.sets.forEach { setFill ->
                    setRepository.addSet(
                        exerciseEntryId = exercise.id,
                        setNumber = nextSetNumber++,
                        repsPerformed = setFill.reps,
                        weight = setFill.weight,
                        weightMode = WeightMode.BARBELL,
                    )
                }
            }
            _autoFillProposal.value = null
        }
    }

    // ── Speech recognition ───────────────────────────────────────────────────────

    fun startListening() {
        if (_micState.value is MicState.Listening) return
        val ctx = getApplication<Application>().applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            _micState.value = MicState.Error("Speech recognition not available on this device.")
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _micState.value = MicState.Listening }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy."
                    SpeechRecognizer.ERROR_SERVER -> "Server error."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input."
                    else -> "Speech error ($error)."
                }
                _micState.value = MicState.Error(msg)
                destroyRecognizer()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull() ?: ""
                _micState.value = MicState.Idle
                destroyRecognizer()
                if (transcript.isNotBlank()) sendMessage(transcript)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _micState.value = MicState.Idle
        destroyRecognizer()
    }

    override fun onCleared() {
        super.onCleared()
        destroyRecognizer()
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private suspend fun buildSystemPrompt(): String {
        val sid = sessionId
        return if (isWorkoutMode && sid != null) {
            buildWorkoutSystemPrompt(sid)
        } else {
            buildHistorySystemPrompt()
        }
    }

    private suspend fun buildWorkoutSystemPrompt(sid: String): String {
        val exercises = workoutRepository.observeExercises(sid).first()
        val contextLines = buildString {
            appendLine("You are a gym coach assistant helping the user log their workout.")
            appendLine()
            appendLine("Current session movements:")
            exercises.forEach { ex ->
                val target = buildString {
                    ex.targetSets?.let { append("${it}x") }
                    if (ex.targetModifier == RepModifier.MAX) append("MAX") else ex.targetReps?.let { append("$it") }
                }
                val sets = setRepository.observeSets(ex.id).first()
                appendLine("- ${ex.exerciseName}${if (target.isNotBlank()) " (target: $target)" else ""}")
                if (sets.isNotEmpty()) {
                    val setStr = sets.joinToString(", ") { s ->
                        "set ${s.setNumber}: ${s.weight?.let { "${formatWeight(it, appendUnit = false)}kg" } ?: ""}×${s.repsPerformed ?: "?"}"
                    }
                    appendLine("  Logged: $setStr")
                } else {
                    appendLine("  No sets logged yet.")
                }
            }
        }
        return contextLines.trimEnd() + """

When the user describes workout results (e.g. "I hit all my sets at 80kg" or "Back squat 4x8 at 80kg"), respond with ONLY valid JSON:
{
  "action": "fill_sets",
  "fills": [
    { "exercise_name": "Back Squat", "sets": [{"reps": 8, "weight": 80.0}, {"reps": 8, "weight": 80.0}] }
  ],
  "message": "Filled 4×8 at 80kg for Back Squat"
}

For general questions or ambiguous input, respond with:
{ "action": "answer", "message": "Your answer here." }

Always return valid JSON only — no markdown, no explanation outside the JSON."""
    }

    private suspend fun buildHistorySystemPrompt(): String {
        val summaries: List<ExerciseProgressSummary> = workoutRepository.observeProgressSummaries(limit = 20).first()
        val contextLines = buildString {
            appendLine("You are a gym coach assistant. Answer questions about the user's workout history.")
            appendLine()
            appendLine("Movement history summary (recent movements):")
            summaries.forEach { s ->
                appendLine("- ${s.exerciseName}: ${s.sessionCount} session(s), best weight ${s.bestWeight?.let { formatWeight(it) } ?: "unknown"}, last logged ${formatDate(s.lastPerformed)}")
            }
        }
        return contextLines.trimEnd() + """

Respond with ONLY valid JSON:
{ "action": "answer", "message": "Your answer here." }

No markdown, no explanation outside the JSON."""
    }

    private fun tryParseAutoFill(reply: String): AutoFillProposal? {
        val stripped = reply.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val root = json.decodeFromString<AiActionResponse>(stripped)
            if (root.action == "fill_sets" && root.fills != null) {
                AutoFillProposal(
                    fills = root.fills.map { fill ->
                        ExerciseFill(
                            exerciseName = fill.exerciseName,
                            sets = fill.sets.map { s -> SetFill(reps = s.reps, weight = s.weight) },
                        )
                    },
                    summary = root.message ?: "",
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryExtractAnswer(reply: String): String {
        val stripped = reply.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val root = json.decodeFromString<AiActionResponse>(stripped)
            root.message ?: reply
        } catch (_: Exception) {
            reply
        }
    }
}

// ── JSON models for AI responses ─────────────────────────────────────────────

@kotlinx.serialization.Serializable
private data class AiActionResponse(
    val action: String = "answer",
    val fills: List<AiExerciseFill>? = null,
    val message: String? = null,
)

@kotlinx.serialization.Serializable
private data class AiExerciseFill(
    @kotlinx.serialization.SerialName("exercise_name") val exerciseName: String,
    val sets: List<AiSetFill> = emptyList(),
)

@kotlinx.serialization.Serializable
private data class AiSetFill(
    val reps: Int? = null,
    val weight: Float? = null,
)
