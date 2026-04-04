package com.gymapp.core.ai

import com.gymapp.core.model.ExerciseEntry

/**
 * Scaffold — AI-powered workout card parser.
 *
 * Will parse an image (bitmap bytes) of a handwritten or printed workout card
 * and return a list of [ExerciseEntry] domain objects.
 *
 * Implementation is deferred to a future milestone.
 */
interface WorkoutCardParser {
    /**
     * @param imageBytes JPEG or PNG bytes captured by CameraX.
     * @return Parsed exercise entries, or a failure with the underlying error.
     */
    suspend fun parse(imageBytes: ByteArray, onProgress: (String) -> Unit = {}): Result<List<ExerciseEntry>>
}
