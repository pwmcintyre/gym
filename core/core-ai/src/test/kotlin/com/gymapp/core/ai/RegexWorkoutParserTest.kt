package com.gymapp.core.ai

import com.gymapp.core.model.RepModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexWorkoutParserTest {

    // -------------------------------------------------------------------------
    // Label extraction
    // -------------------------------------------------------------------------

    @Test
    fun parsesLabelWithSpace() {
        val result = RegexWorkoutParser.parse("A1 Back Squat 4x8")
        assertEquals("A1", result.first().label)
    }

    @Test
    fun parsesLabelWithDot() {
        val result = RegexWorkoutParser.parse("B2. Romanian Deadlift 3x10")
        assertEquals("B2", result.first().label)
    }

    @Test
    fun parsesLabelWithParen() {
        val result = RegexWorkoutParser.parse("C3) Pull-ups 3x max")
        assertEquals("C3", result.first().label)
    }

    @Test
    fun parsesLabelWithDash() {
        val result = RegexWorkoutParser.parse("A2 - Bench Press 4x8")
        assertEquals("A2", result.first().label)
    }

    @Test
    fun emptyLabelWhenNonePresent() {
        val result = RegexWorkoutParser.parse("Back Squat 4x8")
        assertEquals("", result.first().label)
    }

    @Test
    fun lowercaseLabelNormalisedToUpper() {
        val result = RegexWorkoutParser.parse("a1 Back Squat 4x8")
        assertEquals("A1", result.first().label)
    }

    // -------------------------------------------------------------------------
    // Exercise name extraction
    // -------------------------------------------------------------------------

    @Test
    fun parsesExerciseName() {
        val result = RegexWorkoutParser.parse("A1 Back Squat 4x8")
        assertEquals("Back Squat", result.first().exerciseName)
    }

    @Test
    fun parsesMultiWordExerciseName() {
        val result = RegexWorkoutParser.parse("B1 Romanian Deadlift 3x10")
        assertEquals("Romanian Deadlift", result.first().exerciseName)
    }

    @Test
    fun parsesHyphenatedExerciseName() {
        val result = RegexWorkoutParser.parse("B2 Pull-ups 3x8")
        assertEquals("Pull-ups", result.first().exerciseName)
    }

    // -------------------------------------------------------------------------
    // Sets × Reps formats
    // -------------------------------------------------------------------------

    @Test
    fun parsesCompactSetsReps() {
        val result = RegexWorkoutParser.parse("Back Squat 4x8")
        assertEquals(4, result.first().targetSets)
        assertEquals(8, result.first().targetReps)
    }

    @Test
    fun parsesSpacedSetsReps() {
        val result = RegexWorkoutParser.parse("Back Squat 4 x 8")
        assertEquals(4, result.first().targetSets)
        assertEquals(8, result.first().targetReps)
    }

    @Test
    fun parsesUppercaseX() {
        val result = RegexWorkoutParser.parse("Back Squat 4X8")
        assertEquals(4, result.first().targetSets)
        assertEquals(8, result.first().targetReps)
    }

    @Test
    fun parsesMultiplicationSign() {
        val result = RegexWorkoutParser.parse("Back Squat 4×8")
        assertEquals(4, result.first().targetSets)
        assertEquals(8, result.first().targetReps)
    }

    @Test
    fun parsesSetsOfRepsFormat() {
        val result = RegexWorkoutParser.parse("Goblet Squat 4 sets of 10")
        assertEquals(4, result.first().targetSets)
        assertEquals(10, result.first().targetReps)
    }

    @Test
    fun parsesRepRangeUsesFirstNumber() {
        // "3x8-10" — take the first rep count (8)
        val result = RegexWorkoutParser.parse("Ring Dips 3x8-10")
        assertEquals(3, result.first().targetSets)
        assertEquals(8, result.first().targetReps)
    }

    // -------------------------------------------------------------------------
    // MAX / AMRAP modifier
    // -------------------------------------------------------------------------

    @Test
    fun parsesMaxModifierLowercase() {
        val result = RegexWorkoutParser.parse("Pull-ups 3 x max")
        assertEquals(3, result.first().targetSets)
        assertEquals(null, result.first().targetReps)
        assertEquals(RepModifier.MAX, result.first().targetModifier)
    }

    @Test
    fun parsesAmrapModifier() {
        val result = RegexWorkoutParser.parse("Push-ups 3xAMRAP")
        assertEquals(RepModifier.MAX, result.first().targetModifier)
    }

    // -------------------------------------------------------------------------
    // Notes extraction
    // -------------------------------------------------------------------------

    @Test
    fun extractsParenthesisedNotes() {
        val result = RegexWorkoutParser.parse("A1 Back Squat 4x8 (3s pause at bottom)")
        assertEquals("3s pause at bottom", result.first().notes)
    }

    @Test
    fun nullNotesWhenAbsent() {
        val result = RegexWorkoutParser.parse("A1 Back Squat 4x8")
        assertEquals(null, result.first().notes)
    }

    @Test
    fun stripsAtPercentageFromName() {
        // "@80%" should not bleed into the exercise name
        val result = RegexWorkoutParser.parse("Deadlift 3x5 @80%")
        assertEquals("Deadlift", result.first().exerciseName)
    }

    // -------------------------------------------------------------------------
    // Multi-line whiteboard
    // -------------------------------------------------------------------------

    @Test
    fun parsesMultiLineWhiteboard() {
        val whiteboard = """
            A1 Back Squat 4x8
            A2 Romanian Deadlift 3x10
            B1 Pull-ups 3 x max
            B2 Push-ups 4x15
        """.trimIndent()
        val result = RegexWorkoutParser.parse(whiteboard)
        assertEquals(4, result.size)
        assertEquals("Back Squat", result[0].exerciseName)
        assertEquals("Romanian Deadlift", result[1].exerciseName)
        assertEquals(RepModifier.MAX, result[2].targetModifier)
        assertEquals(15, result[3].targetReps)
    }

    @Test
    fun skipsLinesWithNoSetsReps() {
        val whiteboard = """
            Warm-up
            A1 Back Squat 4x8
            Cool down / stretch
        """.trimIndent()
        val result = RegexWorkoutParser.parse(whiteboard)
        assertEquals(1, result.size)
        assertEquals("Back Squat", result[0].exerciseName)
    }

    @Test
    fun returnsEmptyForCompletelyUnstructuredText() {
        val result = RegexWorkoutParser.parse("Today is leg day! Work hard.")
        assertTrue(result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Realistic OCR output samples
    // -------------------------------------------------------------------------

    @Test
    fun parsesRealisticOcrSample() {
        // Simulates what ML Kit might return from a typical gym whiteboard
        val ocr = """
            A1 Back Squat
            4 x 8 (3s pause)
            A2 Romanian Deadlift 3x10
            B1 Weighted Pull-ups 4 x 6
            B2 Dumbbell Row 3 x 12
        """.trimIndent()
        // A1 spans two lines so regex won't parse it (no sets on name line) — expect 3
        val result = RegexWorkoutParser.parse(ocr)
        assertEquals(3, result.size)
        assertEquals("Romanian Deadlift", result[0].exerciseName)
    }

    @Test
    fun handlesMixedCaseExerciseNames() {
        val result = RegexWorkoutParser.parse("BACK SQUAT 4x8")
        assertEquals("BACK SQUAT", result.first().exerciseName)
    }

    @Test
    fun rawSourceTextPreservedFromOriginalLine() {
        val line = "A1 Back Squat 4x8 (3s pause)"
        val result = RegexWorkoutParser.parse(line)
        assertEquals(line, result.first().targetRawText)
    }
}
