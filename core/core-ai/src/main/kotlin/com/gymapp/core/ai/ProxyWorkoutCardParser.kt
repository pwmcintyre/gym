package com.gymapp.core.ai

import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyWorkoutCardParser @Inject constructor(
    private val aiSettings: AiSettings,
) : WorkoutCardParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parse(imageBytes: ByteArray): Result<List<ExerciseEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = aiSettings.proxyUrl.first().trimEnd('/')
                require(baseUrl.isNotBlank()) { "Proxy URL not configured. Set it in Settings." }

                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image",
                        "workout.jpg",
                        imageBytes.toRequestBody("image/jpeg".toMediaType()),
                    )
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/parse-workout")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                check(response.isSuccessful) {
                    "Proxy error ${response.code}: ${response.body?.string()?.take(200)}"
                }

                val responseBody = checkNotNull(response.body?.string()) { "Empty response from proxy" }
                val parsed = json.decodeFromString<ProxyResponse>(responseBody)
                parsed.items.map { it.toExerciseEntry() }
            }
        }
}

@Serializable
private data class ProxyResponse(
    @SerialName("workout_date") val workoutDate: String? = null,
    val items: List<ProxyItem> = emptyList(),
)

@Serializable
private data class ProxyItem(
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
        workoutSessionId = "", // filled in when session is created
        label = label,
        exerciseName = exerciseName,
        targetSets = targetSets,
        targetReps = targetReps,
        targetModifier = if (repModifier.uppercase() == "MAX") RepModifier.MAX else RepModifier.NONE,
        targetRawText = rawSourceText.ifBlank { null },
        notes = notes.ifBlank { null },
    )
}
