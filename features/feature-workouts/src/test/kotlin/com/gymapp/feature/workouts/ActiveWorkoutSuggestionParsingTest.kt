package com.gymapp.feature.workouts

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveWorkoutSuggestionParsingTest {

    @Test
    fun parsesStrictPipeFormat() {
        val parsed = parseSuggestedWorkoutEntries(
            """
            Back Squat | 4 | 6
            Pull Ups | 3 | 8
            """.trimIndent()
        )

        assertEquals(
            listOf(
                SuggestedWorkoutEntry("Back Squat", 4, 6),
                SuggestedWorkoutEntry("Pull Ups", 3, 8),
            ),
            parsed,
        )
    }

    @Test
    fun parsesBulletedReplyWithoutDroppingNames() {
        val parsed = parseSuggestedWorkoutEntries(
            """
            1. DB Shoulder Press | 4 | 8
            - Face Pulls | 3 | 15
            Hollow Hold
            """.trimIndent()
        )

        assertEquals(
            listOf(
                SuggestedWorkoutEntry("DB Shoulder Press", 4, 8),
                SuggestedWorkoutEntry("Face Pulls", 3, 15),
                SuggestedWorkoutEntry("Hollow Hold", null, null),
            ),
            parsed,
        )
    }
}
