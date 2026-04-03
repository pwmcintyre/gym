package com.gymapp.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a single message in a conversation with the AI assistant.
 */
data class ChatMessage(
    val role: Role,
    val content: String,
) {
    enum class Role { USER, ASSISTANT }
}

/**
 * Typed errors surfaced by [AiAssistant] so callers can show specific UX.
 */
sealed class AiException(message: String) : Exception(message) {
    class ApiKeyMissing : AiException("No OpenAI API key configured")
    class Unauthorized : AiException("Invalid or expired API key")
    class RateLimited : AiException("Rate limit exceeded — try again shortly")
    class ServiceError(val code: Int) : AiException("OpenAI service error ($code)")
    class ModelNotReady(reason: String) : AiException(reason)
}

/**
 * Common interface for text-based AI backends.
 * Implementors: [OpenAiAssistant] and [LocalGemmaAssistant].
 * The app-facing [AiAssistant] delegates to whichever backend is active.
 */
interface AiBackend {
    suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        model: String,
    ): Result<String>
}

// ---------------------------------------------------------------------------
// OpenAI backend
// ---------------------------------------------------------------------------

/**
 * Sends text-based chat completion requests to OpenAI.
 */
@Singleton
class OpenAiAssistant @Inject constructor(
    private val aiSettings: AiSettings,
) : AiBackend {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        model: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = aiSettings.apiKey.first()
            if (apiKey.isBlank()) throw AiException.ApiKeyMissing()

            val requestJson = buildJsonObject {
                put("model", model)
                put("max_tokens", 1024)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    messages.forEach { msg ->
                        add(buildJsonObject {
                            put("role", if (msg.role == ChatMessage.Role.USER) "user" else "assistant")
                            put("content", msg.content)
                        })
                    }
                })
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            when (response.code) {
                401 -> throw AiException.Unauthorized()
                429 -> throw AiException.RateLimited()
                in 500..599 -> throw AiException.ServiceError(response.code)
                else -> check(response.isSuccessful) {
                    "OpenAI error ${response.code}: ${response.body?.string()?.take(200)}"
                }
            }

            val body = checkNotNull(response.body?.string()) { "Empty response from OpenAI" }
            val jsonBody = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<AiChatCompletion>(body)
            jsonBody.choices.firstOrNull()?.message?.content
                ?: error("No content in OpenAI response")
        }
    }
}

// ---------------------------------------------------------------------------
// Local Gemma backend
// ---------------------------------------------------------------------------

/**
 * Text-based AI backend backed by the on-device [LocalGemmaEngine].
 * The [model] parameter is ignored — it always uses Gemma 4 E2B.
 */
@Singleton
class LocalGemmaAssistant @Inject constructor(
    private val engine: LocalGemmaEngine,
) : AiBackend {
    override suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        model: String,
    ): Result<String> = engine.chat(systemPrompt, messages)
}

// ---------------------------------------------------------------------------
// Smart dispatcher — picks the right backend based on API key presence
// ---------------------------------------------------------------------------

/**
 * Delegates to [OpenAiAssistant] when an API key is configured, otherwise
 * falls back to [LocalGemmaAssistant].
 *
 * This is the singleton that all app code should inject via [AiAssistant].
 */
@Singleton
class AiAssistant @Inject constructor(
    private val aiSettings: AiSettings,
    private val openAi: OpenAiAssistant,
    private val localGemma: LocalGemmaAssistant,
) {
    suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
        model: String = "gpt-4o",
    ): Result<String> {
        val apiKey = aiSettings.apiKey.first()
        val backend: AiBackend = if (apiKey.isNotBlank()) openAi else localGemma
        return backend.chat(systemPrompt, messages, model)
    }
}

@kotlinx.serialization.Serializable
internal data class AiChatCompletion(val choices: List<AiChoice>)

@kotlinx.serialization.Serializable
internal data class AiChoice(val message: AiMessage)

@kotlinx.serialization.Serializable
internal data class AiMessage(val content: String)
