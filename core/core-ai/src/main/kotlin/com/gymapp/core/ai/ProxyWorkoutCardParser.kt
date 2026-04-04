package com.gymapp.core.ai

import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.extractModifierTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"

private const val SYSTEM_PROMPT = """You are a gym workout parser. Extract the workout from the whiteboard photo into structured JSON.

Return ONLY valid JSON — no markdown, no explanation:
{
  "workout_date": null,
  "items": [
    {
      "label": "A1",
      "exercise_name": "Back Squat",
      "target_sets": 4,
      "target_reps": 8,
      "rep_modifier": "NONE",
      "notes": "3s pause at bottom",
      "raw_source_text": "A1 Back Squat 4 x 8 3s pause at bottom"
    }
  ]
}

Rules:
- label: workout label (A1, B2, etc.) or empty string
- target_sets / target_reps: integer or null; for ranges like "3-4" use the higher number
- rep_modifier: "NONE" or "MAX" (use MAX for "max" / "amrap")
- notes: extra instructions or empty string
- raw_source_text: original text from the board for this item
- workout_date: ISO date string if visible, otherwise null
If unreadable, return {"workout_date": null, "items": []}."""

@Singleton
class ProxyWorkoutCardParser @Inject constructor(
    private val aiSettings: AiSettings,
) : WorkoutCardParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parse(imageBytes: ByteArray, onProgress: (String) -> Unit): Result<List<ExerciseEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiKey = aiSettings.apiKey.first()
                require(apiKey.isNotBlank()) { "OpenAI API key not configured. Set it in Settings." }
                onProgress("Uploading image…")

                val b64 = Base64.getEncoder().encodeToString(imageBytes)

                val requestJson = buildJsonObject {
                    put("model", "gpt-4o")
                    put("max_tokens", 1024)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        })
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", buildJsonArray {
                                add(buildJsonObject {
                                    put("type", "image_url")
                                    put("image_url", buildJsonObject {
                                        put("url", "data:image/jpeg;base64,$b64")
                                        put("detail", "high")
                                    })
                                })
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", "Parse this workout board.")
                                })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url(OPENAI_URL)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                check(response.isSuccessful) {
                    "OpenAI error ${response.code}: ${response.body?.string()?.take(200)}"
                }

                onProgress("Reading response…")
                val body = checkNotNull(response.body?.string()) { "Empty response from OpenAI" }
                parseExerciseEntriesFromCompletionBody(body, json)
            }
        }
}

internal fun parseExerciseEntriesFromCompletionBody(
    body: String,
    json: Json = Json { ignoreUnknownKeys = true },
): List<ExerciseEntry> {
    val completion = json.decodeFromString<ChatCompletion>(body)
    val content = completion.choices.firstOrNull()?.message?.content
        ?: error("No content in OpenAI response")
    return parseExerciseEntriesFromContent(content, json)
}

internal fun parseExerciseEntriesFromContent(
    content: String,
    json: Json = Json { ignoreUnknownKeys = true },
): List<ExerciseEntry> {
    val stripped = content.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    // Gemma (and other small models) often emit preamble/postamble text around the JSON.
    // Grab just the outermost { ... } to be safe.
    val start = stripped.indexOf('{')
    val end = stripped.lastIndexOf('}')
    val jsonOnly = if (start != -1 && end > start) stripped.substring(start, end + 1) else stripped
    val parsed = json.decodeFromString<WorkoutResponse>(jsonOnly)
    return parsed.items.map { it.toExerciseEntry() }
}

@Serializable
private data class ChatCompletion(val choices: List<Choice>)

@Serializable
private data class Choice(val message: Message)

@Serializable
private data class Message(val content: String)

@Serializable
private data class WorkoutResponse(
    @SerialName("workout_date") val workoutDate: String? = null,
    val items: List<WorkoutItem> = emptyList(),
)

@Serializable
private data class WorkoutItem(
    val label: String = "",
    @SerialName("exercise_name") val exerciseName: String = "",
    @SerialName("target_sets") val targetSets: Int? = null,
    @SerialName("target_reps") val targetReps: Int? = null,
    @SerialName("rep_modifier") val repModifier: String = "NONE",
    val notes: String = "",
    @SerialName("raw_source_text") val rawSourceText: String = "",
) {
    fun toExerciseEntry() = ExerciseEntry(
        id = UUID.randomUUID().toString(),
        workoutSessionId = "",
        label = label,
        exerciseName = exerciseName,
        targetSets = targetSets,
        targetReps = targetReps,
        targetModifier = if (repModifier.uppercase() == "MAX") RepModifier.MAX else RepModifier.NONE,
        targetRawText = rawSourceText.ifBlank { null },
        modifierTags = extractModifierTags(exerciseName, notes),
        notes = notes.ifBlank { null },
    )
}
