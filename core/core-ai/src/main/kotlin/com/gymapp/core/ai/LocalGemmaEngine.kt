package com.gymapp.core.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
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
            initMutex.withLock {
                ensureInitialised()
            }

            val currentEngine = engine ?: error("Gemma engine not initialised")

            // Each call gets a fresh conversation so there is no leaking history.
            val conversation = currentEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                ),
            )

            val response = conversation.sendMessage(userPrompt)
            response.extractText() ?: error("Empty response from Gemma")
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
            initMutex.withLock {
                ensureInitialised()
            }

            val currentEngine = engine ?: error("Gemma engine not initialised")

            val conversation = currentEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                ),
            )

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

            val response = conversation.sendMessage(fullPrompt)
            response.extractText() ?: error("Empty response from Gemma")
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

        val eng = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
            ),
        )
        eng.initialize()
        engine = eng
    }
}
