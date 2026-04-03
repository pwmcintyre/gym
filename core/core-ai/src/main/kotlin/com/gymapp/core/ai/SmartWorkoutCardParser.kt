package com.gymapp.core.ai

import com.gymapp.core.model.ExerciseEntry
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches workout card parsing to the right backend:
 * - **OpenAI** when an API key is configured (vision-capable, sends JPEG directly).
 * - **Local Gemma** otherwise (OCR via ML Kit + text-only Gemma).
 *
 * This is the binding that all callers receive via Hilt's [WorkoutCardParser] injection.
 */
@Singleton
class SmartWorkoutCardParser @Inject constructor(
    private val aiSettings: AiSettings,
    private val openAiParser: ProxyWorkoutCardParser,
    private val localParser: LocalWorkoutCardParser,
) : WorkoutCardParser {

    override suspend fun parse(imageBytes: ByteArray): Result<List<ExerciseEntry>> {
        val apiKey = aiSettings.apiKey.first()
        return if (apiKey.isNotBlank()) {
            openAiParser.parse(imageBytes)
        } else {
            localParser.parse(imageBytes)
        }
    }
}
