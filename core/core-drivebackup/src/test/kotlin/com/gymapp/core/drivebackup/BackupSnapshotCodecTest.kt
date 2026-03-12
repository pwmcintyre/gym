package com.gymapp.core.drivebackup

import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.ExerciseTemplate
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.SetEntry
import com.gymapp.core.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSnapshotCodecTest {
    private val codec = BackupSnapshotCodec()

    @Test
    fun encodeDecodeRoundTripPreservesSnapshot() {
        val snapshot = BackupSnapshot(
            exportedAtEpochMillis = 1_710_000_000_000,
            exerciseTemplates = listOf(
                ExerciseTemplate(
                    id = "template-1",
                    name = "Back Squat",
                    defaultNotes = "heels down",
                ),
            ),
            workoutSessions = listOf(
                WorkoutSession(
                    id = "session-1",
                    date = 1_710_000_100_000,
                    notes = "heavy day",
                    createdAt = 1_710_000_100_123,
                ),
            ),
            exerciseEntries = listOf(
                ExerciseEntry(
                    id = "exercise-1",
                    workoutSessionId = "session-1",
                    label = "A1",
                    exerciseName = "Back Squat",
                    targetSets = 4,
                    targetReps = 5,
                    targetModifier = RepModifier.NONE,
                    targetRawText = "4 x 5",
                    notes = "belt on top set",
                ),
            ),
            setEntries = listOf(
                SetEntry(
                    id = "set-1",
                    exerciseEntryId = "exercise-1",
                    setNumber = 1,
                    repsPerformed = 5,
                    weight = 140f,
                    notes = null,
                ),
            ),
        )

        val decoded = codec.decode(codec.encode(snapshot))

        assertEquals(snapshot, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeRejectsUnsupportedSchemaVersion() {
        val unsupported = """
            {
              "schemaVersion": 99,
              "exportedAtEpochMillis": 1710000000000,
              "exerciseTemplates": [],
              "workoutSessions": [],
              "exerciseEntries": [],
              "setEntries": []
            }
        """.trimIndent().toByteArray()

        codec.decode(unsupported)
    }
}
