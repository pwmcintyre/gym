package com.gymapp.core.ai

import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.extractModifierTags
import java.util.UUID

/**
 * Fast, zero-latency workout parser that uses regex to extract structured exercise data
 * from OCR'd whiteboard text.
 *
 * Handles the common gym whiteboard format:
 *   [Label] Exercise Name  Sets x Reps  [(notes)]
 *
 * Examples handled:
 *   A1 Back Squat 4x8
 *   A1. Back Squat 4 x 8
 *   B2) Romanian Deadlift 3 x 10 (3s pause)
 *   Pull-ups 3 x max
 *   Goblet Squat 4 sets x 10 reps
 *   C1 Ring Dips 3x8-10
 *   Deadlift 3x5 @ 80%
 *
 * Returns an empty list when it cannot parse any exercises — callers should
 * fall back to an LLM parser in that case.
 */
object RegexWorkoutParser {

    // Optional label: A1, B2, C3, A1., A1), A1:
    private val LABEL_RE = Regex("""^([A-Za-z]\d+)[.):,\-\s]\s*""")

    // Form A — "4x8", "4 x 8", "4X8", "4×8", "4 sets x 8"
    // Also handles rep ranges like "4x8-10" (captures the lower bound).
    private val SETS_X_REPS_RE = Regex(
        """(\d+)\s*(?:sets?\s*)?[xX×]\s*(\d+)(?:\s*[-–]\s*\d+)?""",
        RegexOption.IGNORE_CASE,
    )

    // Form B — "4 sets of 8", "4 sets of 8 reps", "4 sets by 8"
    private val SETS_OF_REPS_RE = Regex(
        """(\d+)\s+sets?\s+(?:of|by)\s+(\d+)(?:\s*reps?)?""",
        RegexOption.IGNORE_CASE,
    )

    // Max/AMRAP — "3 x max", "3xAMRAP", "3 x amrap"
    private val SETS_MAX_RE = Regex(
        """(\d+)\s*[xX×]\s*(max|amrap)""",
        RegexOption.IGNORE_CASE,
    )

    // Notes in parentheses or after @
    private val PARENS_RE = Regex("""\(([^)]+)\)""")
    private val AT_RE = Regex("""@\s*[\w%.]+""")

    fun parse(ocrText: String, workoutSessionId: String = ""): List<ExerciseEntry> =
        ocrText.lines()
            .map { it.trim() }
            .filter { it.length > 3 }
            .mapNotNull { parseLine(it, workoutSessionId) }

    private fun parseLine(line: String, workoutSessionId: String): ExerciseEntry? {
        var text = line

        // Extract label
        val labelMatch = LABEL_RE.find(text)
        val label = labelMatch?.groupValues?.get(1)?.uppercase() ?: ""
        if (labelMatch != null) text = text.removeRange(labelMatch.range).trim()

        // Extract parenthesised notes
        val notes = PARENS_RE.find(text)?.groupValues?.get(1)?.trim() ?: ""
        text = PARENS_RE.replace(text, "").trim()
        text = AT_RE.replace(text, "").trim()

        // Find sets x reps — try each form in order of specificity
        val setsXMatch = SETS_X_REPS_RE.find(text)
        val setsOfMatch = SETS_OF_REPS_RE.find(text)
        val setsMaxMatch = SETS_MAX_RE.find(text)

        val targetSets: Int?
        val targetReps: Int?
        val repModifier: RepModifier
        val matchRange: IntRange

        when {
            setsXMatch != null -> {
                targetSets = setsXMatch.groupValues[1].toIntOrNull()
                targetReps = setsXMatch.groupValues[2].toIntOrNull()
                repModifier = RepModifier.NONE
                matchRange = setsXMatch.range
            }
            setsOfMatch != null -> {
                targetSets = setsOfMatch.groupValues[1].toIntOrNull()
                targetReps = setsOfMatch.groupValues[2].toIntOrNull()
                repModifier = RepModifier.NONE
                matchRange = setsOfMatch.range
            }
            setsMaxMatch != null -> {
                targetSets = setsMaxMatch.groupValues[1].toIntOrNull()
                targetReps = null
                repModifier = RepModifier.MAX
                matchRange = setsMaxMatch.range
            }
            else -> return null // no sets/reps found — skip
        }

        // Exercise name is whatever comes before the sets/reps match
        val exerciseName = text.substring(0, matchRange.first)
            .trim()
            .trimEnd(',', '.', ':', '-', '–')
            .trim()

        if (exerciseName.isBlank() || exerciseName.length < 2) return null

        return ExerciseEntry(
            id = UUID.randomUUID().toString(),
            workoutSessionId = workoutSessionId,
            label = label,
            exerciseName = exerciseName,
            targetSets = targetSets,
            targetReps = targetReps,
            targetModifier = repModifier,
            targetRawText = line,
            modifierTags = extractModifierTags(exerciseName, notes),
            notes = notes.ifBlank { null },
        )
    }
}
