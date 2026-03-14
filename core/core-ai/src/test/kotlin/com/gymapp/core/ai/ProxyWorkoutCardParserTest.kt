package com.gymapp.core.ai

import com.gymapp.core.model.RepModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyWorkoutCardParserTest {

    @Test
    fun parsesWellFormedResponseIntoWorkoutEntries() {
        val entries = parseExerciseEntriesFromCompletionBody(
            """
            {
              "choices": [
                {
                  "message": {
                    "content": "```json\n{\"workout_date\":null,\"items\":[{\"label\":\"A1\",\"exercise_name\":\"Back Squat\",\"target_sets\":4,\"target_reps\":8,\"rep_modifier\":\"MAX\",\"notes\":\"3s pause at bottom\",\"raw_source_text\":\"A1 Back Squat 4 x max 3s pause at bottom\"}]}\n```"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("A1", entry.label)
        assertEquals("Back Squat", entry.exerciseName)
        assertEquals(4, entry.targetSets)
        assertEquals(8, entry.targetReps)
        assertEquals(RepModifier.MAX, entry.targetModifier)
        assertEquals("A1 Back Squat 4 x max 3s pause at bottom", entry.targetRawText)
        assertEquals("3s pause at bottom", entry.notes)
        assertTrue("Pause" in entry.modifierTags)
    }

    @Test
    fun malformedJsonFailsGracefully() {
        val result = runCatching {
            parseExerciseEntriesFromContent(
                """{"workout_date":null,"items":[{"label":"A1","exercise_name":"Bench Press"}"""
            )
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun emptyItemsResponseReturnsEmptyList() {
        val entries = parseExerciseEntriesFromContent(
            """{"workout_date":null,"items":[]}"""
        )

        assertTrue(entries.isEmpty())
    }

    @Test
    fun missingCompletionContentFailsGracefully() {
        val result = runCatching {
            parseExerciseEntriesFromCompletionBody("""{"choices":[]}""")
        }

        assertTrue(result.isFailure)
        assertEquals("No content in OpenAI response", result.exceptionOrNull()?.message)
    }
}
