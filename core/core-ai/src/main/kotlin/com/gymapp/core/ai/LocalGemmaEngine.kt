package com.gymapp.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Extract all text parts from a [Message] response, joined with newline. */
private fun Message.extractText(): String? =
    contents.contents
        .filterIsInstance<Content.Text>()
        .joinToString("\n") { it.text }
        .takeIf { it.isNotBlank() }

/**
 * Manages the lifecycle of the on-device LiteRT-LM [Engine] for Gemma 4 E2B.
 *
 * The engine is lazy-initialised on first use (initialization can take ~5-10s) and
 * is shared across all callers. Callers must hold [initMutex] across chat calls to
 * prevent concurrent Conversation objects from interfering.
 *
 * This class does NOT handle model download — that is [ModelDownloadManager]'s job.
 * If the model file is not present, [chat] returns a failure.
 */
@Singleton
class LocalGemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
) {
    private var engine: Engine? = null
    private val initMutex = Mutex()

    /**
     * True if the engine has been successfully initialised.
     */
    val isInitialized: Boolean get() = engine != null

    /**
     * Send a single-turn prompt to Gemma and return the full response text.
     * Initialises the engine lazily on first call.
     *
     * @param systemPrompt Instruction prepended as system turn.
     * @param userPrompt   The actual user content.
     */
    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Hold the mutex for the full conversation lifecycle — LiteRT-LM only supports
            // one active session at a time; concurrent calls produce FAILED_PRECONDITION.
            initMutex.withLock {
                ensureInitialised()
                val currentEngine = engine ?: error("Gemma engine not initialised")

                // Each call gets a fresh conversation so there is no leaking history.
                // close() is required after use — Engine tracks open conversations natively
                // and throws FAILED_PRECONDITION if a prior one is not closed first.
                currentEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                    ),
                ).use { conversation ->
                    val response = conversation.sendMessage(userPrompt)
                    response.extractText() ?: error("Empty response from Gemma")
                }
            }
        }
    }

    /**
     * Multi-turn chat: sends the full conversation history to Gemma.
     * The system prompt is injected as the conversation system instruction.
     */
    suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Hold the mutex for the full conversation lifecycle — LiteRT-LM only supports
            // one active session at a time; concurrent calls produce FAILED_PRECONDITION.
            initMutex.withLock {
                ensureInitialised()
                val currentEngine = engine ?: error("Gemma engine not initialised")

                // Replay all prior messages so Gemma has context.
                // Build a combined text prompt that includes prior turns.
                val historyText = buildString {
                    messages.dropLast(1).forEach { msg ->
                        val label = if (msg.role == ChatMessage.Role.USER) "User" else "Assistant"
                        appendLine("$label: ${msg.content}")
                    }
                }

                val lastUser = messages.lastOrNull { it.role == ChatMessage.Role.USER }
                    ?: error("No user message in conversation")

                val fullPrompt = if (historyText.isNotBlank()) {
                    "$historyText\nUser: ${lastUser.content}"
                } else {
                    lastUser.content
                }

                currentEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(systemPrompt),
                    ),
                ).use { conversation ->
                    val response = conversation.sendMessage(fullPrompt)
                    response.extractText() ?: error("Empty response from Gemma")
                }
            }
        }
    }

    /** Release engine resources. */
    fun close() {
        engine?.close()
        engine = null
    }

    // Must be called inside initMutex
    private fun ensureInitialised() {
        if (engine != null) return

        val modelPath = modelDownloadManager.modelPath()
            ?: error("Gemma model not downloaded. Go to Settings → AI Model to download it.")

        // Prefer GPU for ~3–5× faster inference; fall back to CPU if unavailable.
        val cpuThreads = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
        engine = tryInit(modelPath, Backend.GPU())
            ?: tryInit(modelPath, Backend.CPU(numOfThreads = cpuThreads))
            ?: error("Failed to initialise Gemma engine on GPU and CPU")
    }

    private fun tryInit(modelPath: String, backend: Backend): Engine? = try {
        Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = backend,
                // Cache compiled GPU/CPU kernels so subsequent inits are faster.
                cacheDir = context.cacheDir.absolutePath,
            ),
        ).also { it.initialize() }
    } catch (_: Exception) {
        null
    }
}
