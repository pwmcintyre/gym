package com.gymapp.feature.workouts

import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.WeightMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class WorkoutCreationTest {

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var setRepository: SetRepository

    @Before
    fun setUp() {
        workoutRepository = WorkoutRepository(FakeWorkoutSessionDao(), FakeExerciseEntryDao())
        setRepository = SetRepository(FakeSetEntryDao())
    }

    // ── Creating a new workout session ───────────────────────────────────────

    @Test
    fun createWorkoutSession_returnsSessionWithAssignedId() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)

        assertNotNull(session.id)
        assertEquals(1_700_000_000_000L, session.date)
    }

    @Test
    fun createWorkoutSession_isObservableViaFlow() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)

        val all = workoutRepository.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(session.id, all[0].id)
    }

    @Test
    fun createWorkoutSession_withNotes_notesArePersisted() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L, notes = "Push")

        val loaded = workoutRepository.getById(session.id)
        assertEquals("Push", loaded?.notes)
    }

    // ── Adding a set to an existing session ──────────────────────────────────

    @Test
    fun addSetToExistingSession_setIsObservableWithCorrectValues() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)
        val exercise = workoutRepository.addExercise(
            sessionId = session.id,
            label = "A1",
            exerciseName = "Bench Press",
        )

        val addedSet = setRepository.addSet(
            exerciseEntryId = exercise.id,
            setNumber = 1,
            repsPerformed = 5,
            weight = 100f,
            weightMode = WeightMode.BARBELL,
        )

        val sets = setRepository.observeSets(exercise.id).first()
        assertEquals(1, sets.size)
        assertEquals(addedSet.id, sets[0].id)
        assertEquals(5, sets[0].repsPerformed)
        assertEquals(100f, sets[0].weight)
        assertEquals(WeightMode.BARBELL, sets[0].weightMode)
    }

    @Test
    fun addMultipleSets_orderedBySetNumber() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)
        val exercise = workoutRepository.addExercise(
            sessionId = session.id,
            label = "A1",
            exerciseName = "Squat",
        )

        setRepository.addSet(exerciseEntryId = exercise.id, setNumber = 2, weight = 80f, repsPerformed = 8)
        setRepository.addSet(exerciseEntryId = exercise.id, setNumber = 1, weight = 60f, repsPerformed = 10)

        val sets = setRepository.observeSets(exercise.id).first()
        assertEquals(2, sets.size)
        assertEquals(1, sets[0].setNumber)
        assertEquals(2, sets[1].setNumber)
    }

    // ── Editing set values (weight, reps, modifier) ───────────────────────────

    @Test
    fun editSetValues_updatesWeightRepsAndWeightMode() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)
        val exercise = workoutRepository.addExercise(
            sessionId = session.id,
            label = "A1",
            exerciseName = "Deadlift",
        )
        val set = setRepository.addSet(
            exerciseEntryId = exercise.id,
            setNumber = 1,
            repsPerformed = 5,
            weight = 80f,
            weightMode = WeightMode.BARBELL,
        )

        setRepository.update(set.copy(repsPerformed = 8, weight = 90f, weightMode = WeightMode.BODYWEIGHT))

        val sets = setRepository.observeSets(exercise.id).first()
        assertEquals(1, sets.size)
        assertEquals(8, sets[0].repsPerformed)
        assertEquals(90f, sets[0].weight)
        assertEquals(WeightMode.BODYWEIGHT, sets[0].weightMode)
    }

    @Test
    fun editSetValues_onlyTargetedSetIsModified() = runTest {
        val session = workoutRepository.create(date = 1_700_000_000_000L)
        val exercise = workoutRepository.addExercise(
            sessionId = session.id,
            label = "A1",
            exerciseName = "OHP",
        )
        val set1 = setRepository.addSet(exercise.id, setNumber = 1, weight = 50f, repsPerformed = 5)
        setRepository.addSet(exercise.id, setNumber = 2, weight = 50f, repsPerformed = 5)

        setRepository.update(set1.copy(weight = 55f, repsPerformed = 4))

        val sets = setRepository.observeSets(exercise.id).first()
        assertEquals(55f, sets[0].weight)
        assertEquals(4, sets[0].repsPerformed)
        // set2 unchanged
        assertEquals(50f, sets[1].weight)
        assertEquals(5, sets[1].repsPerformed)
    }
}
