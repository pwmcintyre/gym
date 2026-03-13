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
 * Sends text-based chat completion requests to OpenAI.
 * Context (workout state or history) is injected as a system prompt.
 */
@Singleton
class AiAssistant @Inject constructor(
    private val aiSettings: AiSettings,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends [messages] (with [systemPrompt] prepended) to GPT-4o and returns the assistant reply.
     */
    suspend fun chat(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = aiSettings.apiKey.first()
            require(apiKey.isNotBlank()) { "OpenAI API key not configured. Set it in Settings." }

            val requestJson = buildJsonObject {
                put("model", "gpt-4o")
                put("max_tokens", 1024)
                put("messages", buildJsonArray {
                    // System context
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    // Conversation history + current user message
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
            check(response.isSuccessful) {
                "OpenAI error ${response.code}: ${response.body?.string()?.take(200)}"
            }

            val body = checkNotNull(response.body?.string()) { "Empty response from OpenAI" }
            // Parse via minimal deserialization (reuse the same pattern as ProxyWorkoutCardParser)
            val jsonBody = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<AiChatCompletion>(body)
            jsonBody.choices.firstOrNull()?.message?.content
                ?: error("No content in OpenAI response")
        }
    }
}

@kotlinx.serialization.Serializable
internal data class AiChatCompletion(val choices: List<AiChoice>)

@kotlinx.serialization.Serializable
internal data class AiChoice(val message: AiMessage)

@kotlinx.serialization.Serializable
internal data class AiMessage(val content: String)
