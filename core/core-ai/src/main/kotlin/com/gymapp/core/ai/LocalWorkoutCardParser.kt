package com.gymapp.core.ai

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gymapp.core.model.ExerciseEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implements [WorkoutCardParser] using:
 * 1. **ML Kit text recognition** to OCR the whiteboard image into plain text.
 * 2. **Local Gemma 4 E2B** (via [LocalGemmaEngine]) to parse that text into workout JSON.
 *
 * Gemma 4 is text-only, so we extract text first rather than sending the image directly.
 */
@Singleton
class LocalWorkoutCardParser @Inject constructor(
    private val engine: LocalGemmaEngine,
) : WorkoutCardParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun parse(imageBytes: ByteArray): Result<List<ExerciseEntry>> =
        withContext(Dispatchers.Default) {
            runCatching {
                // Step 1: OCR
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: error("Could not decode image bytes into a bitmap")
                val image = InputImage.fromBitmap(bitmap, 0)
                val ocrText = recognizeText(image)

                if (ocrText.isBlank()) {
                    return@runCatching emptyList()
                }

                // Step 2: Gemma parses the text into JSON
                val result = engine.chat(
                    systemPrompt = SCAN_SYSTEM_PROMPT,
                    userPrompt = "Parse this workout board text:\n\n$ocrText",
                )

                val content = result.getOrThrow()
                parseExerciseEntriesFromContent(content)
            }
        }

    /** Wraps the ML Kit Task API into a coroutine suspend function. */
    private suspend fun recognizeText(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener { exception ->
                    cont.resumeWithException(exception)
                }
        }
}

private const val SCAN_SYSTEM_PROMPT = """You are a gym workout parser. Extract the workout from the text into structured JSON.

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
