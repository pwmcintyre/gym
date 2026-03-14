package com.gymapp.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymapp.core.database.dao.ExerciseEntryDao
import com.gymapp.core.database.dao.SetEntryDao
import com.gymapp.core.database.dao.WorkoutSessionDao
import com.gymapp.core.database.entity.ExerciseEntryEntity
import com.gymapp.core.database.entity.SetEntryEntity
import com.gymapp.core.database.entity.WorkoutSessionEntity
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.WeightMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MovementHistoryTest {

    private lateinit var db: GymDatabase
    private lateinit var sessionDao: WorkoutSessionDao
    private lateinit var exerciseDao: ExerciseEntryDao
    private lateinit var setDao: SetEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = db.workoutSessionDao()
        exerciseDao = db.exerciseEntryDao()
        setDao = db.setEntryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private suspend fun session(id: String, date: Long) {
        sessionDao.insert(WorkoutSessionEntity(id = id, date = date, notes = null, createdAt = date))
    }

    private suspend fun exercise(id: String, sessionId: String, name: String) {
        exerciseDao.insert(
            ExerciseEntryEntity(
                id = id,
                workoutSessionId = sessionId,
                label = "A1",
                exerciseName = name,
                targetSets = null,
                targetReps = null,
                targetModifier = RepModifier.NONE,
                targetRawText = null,
                modifierTags = emptyList(),
                notes = null,
            )
        )
    }

    private suspend fun set(
        id: String,
        exerciseId: String,
        setNumber: Int,
        weight: Float?,
        reps: Int?,
        mode: WeightMode = WeightMode.BARBELL,
    ) {
        setDao.insert(
            SetEntryEntity(
                id = id,
                exerciseEntryId = exerciseId,
                setNumber = setNumber,
                repsPerformed = reps,
                weight = weight,
                notes = null,
                weightMode = mode,
            )
        )
    }

    // ── retrieveMovementHistory — no sessions ────────────────────────────────

    @Test
    fun `no sessions returns empty history`() = runTest {
        val result = exerciseDao.observeProgressSessions("Bench Press").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `exercise that has never been logged returns empty history`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Squat")
        set("set1", "e1", 1, 100f, 5)

        val result = exerciseDao.observeProgressSessions("Bench Press").first()
        assertTrue(result.isEmpty())
    }

    // ── retrieveMovementHistory — single session ─────────────────────────────

    @Test
    fun `single session with one set returns one progress entry`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Bench Press")
        set("set1", "e1", 1, 80f, 5)

        val result = exerciseDao.observeProgressSessions("Bench Press").first()

        assertEquals(1, result.size)
        assertEquals("s1", result[0].sessionId)
        assertEquals(1_000L, result[0].sessionDate)
        assertEquals(1, result[0].setCount)
        assertEquals(80f, result[0].bestWeight)
        assertEquals(5, result[0].bestReps)
    }

    // ── best set per session ─────────────────────────────────────────────────

    @Test
    fun `best weight and best reps are taken independently per session`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Squat")
        set("set1", "e1", 1, 100f, 5)
        set("set2", "e1", 2, 110f, 3)  // heaviest
        set("set3", "e1", 3, 80f, 8)   // most reps

        val result = exerciseDao.observeProgressSessions("Squat").first()

        assertEquals(1, result.size)
        assertEquals(3, result[0].setCount)
        assertEquals(110f, result[0].bestWeight)
        assertEquals(8, result[0].bestReps)
    }

    @Test
    fun `each session has its own best set — not cross-session max`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Deadlift")
        set("set1", "e1", 1, 120f, 3)

        session("s2", 2_000L)
        exercise("e2", "s2", "Deadlift")
        set("set2", "e2", 1, 130f, 2)
        set("set3", "e2", 2, 125f, 4)

        val result = exerciseDao.observeProgressSessions("Deadlift").first()

        // s2 (most recent) first
        assertEquals(2, result.size)
        assertEquals("s2", result[0].sessionId)
        assertEquals(130f, result[0].bestWeight)
        assertEquals(4, result[0].bestReps)
        assertEquals("s1", result[1].sessionId)
        assertEquals(120f, result[1].bestWeight)
        assertEquals(3, result[1].bestReps)
    }

    // ── ordering by date ─────────────────────────────────────────────────────

    @Test
    fun `sessions returned in descending date order — most recent first`() = runTest {
        // Insert out of chronological order
        session("s3", 3_000L)
        session("s1", 1_000L)
        session("s2", 2_000L)
        exercise("e1", "s1", "Pull Up")
        exercise("e2", "s2", "Pull Up")
        exercise("e3", "s3", "Pull Up")
        set("set1", "e1", 1, null, 8, WeightMode.BODYWEIGHT)
        set("set2", "e2", 1, null, 10, WeightMode.BODYWEIGHT)
        set("set3", "e3", 1, null, 12, WeightMode.BODYWEIGHT)

        val result = exerciseDao.observeProgressSessions("Pull Up").first()

        assertEquals(3, result.size)
        assertEquals(3_000L, result[0].sessionDate)
        assertEquals(2_000L, result[1].sessionDate)
        assertEquals(1_000L, result[2].sessionDate)
    }

    // ── multiple exercises — isolation ────────────────────────────────────────

    @Test
    fun `history only includes sessions for the requested exercise`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Bench Press")
        exercise("e2", "s1", "Overhead Press")
        set("set1", "e1", 1, 80f, 5)
        set("set2", "e2", 1, 60f, 5)

        val bench = exerciseDao.observeProgressSessions("Bench Press").first()
        val ohp = exerciseDao.observeProgressSessions("Overhead Press").first()

        assertEquals(1, bench.size)
        assertEquals(80f, bench[0].bestWeight)
        assertEquals(1, ohp.size)
        assertEquals(60f, ohp[0].bestWeight)
    }

    // ── getPreviousSessionSets ────────────────────────────────────────────────

    @Test
    fun `getPreviousSessionSets returns sets from most recent prior session`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Bench Press")
        set("set1", "e1", 1, 70f, 5)

        session("s2", 2_000L)
        exercise("e2", "s2", "Bench Press")
        set("set2", "e2", 1, 80f, 5)
        set("set3", "e2", 2, 85f, 3)

        // s3 is current — should be excluded
        session("s3", 3_000L)
        exercise("e3", "s3", "Bench Press")

        val prev = setDao.getPreviousSessionSets("Bench Press", "s3")

        assertEquals(2, prev.size)
        assertEquals(80f, prev[0].weight)
        assertEquals(85f, prev[1].weight)
    }

    @Test
    fun `getPreviousSessionSets returns empty when no prior sessions`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Bench Press")

        val prev = setDao.getPreviousSessionSets("Bench Press", "s1")
        assertTrue(prev.isEmpty())
    }

    // ── getPreviousSessionDate ────────────────────────────────────────────────

    @Test
    fun `getPreviousSessionDate returns null when exercise has no prior history`() = runTest {
        session("s1", 1_000L)
        val date = setDao.getPreviousSessionDate("Bench Press", "s1")
        assertNull(date)
    }

    @Test
    fun `getPreviousSessionDate returns date of most recent prior session`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Squat")
        set("set1", "e1", 1, 100f, 5)

        session("s2", 2_000L)
        exercise("e2", "s2", "Squat")
        set("set2", "e2", 1, 105f, 5)

        session("s3", 3_000L)
        exercise("e3", "s3", "Squat")

        val date = setDao.getPreviousSessionDate("Squat", "s3")
        assertEquals(2_000L, date)
    }

    // ── observeProgressSummaries ──────────────────────────────────────────────

    @Test
    fun `progress summary aggregates session count across multiple sessions`() = runTest {
        session("s1", 1_000L)
        exercise("e1", "s1", "Pull Up")
        set("set1", "e1", 1, null, 8, WeightMode.BODYWEIGHT)

        session("s2", 2_000L)
        exercise("e2", "s2", "Pull Up")
        set("set2", "e2", 1, 10f, 6)

        val summaries = exerciseDao.observeProgressSummaries(10).first()
        val pullUp = summaries.find { it.exerciseName == "Pull Up" }

        assertNotNull(pullUp)
        assertEquals(2, pullUp!!.sessionCount)
        assertEquals(2_000L, pullUp.lastPerformed)
    }

    @Test
    fun `progress summaries are empty when database is empty`() = runTest {
        val summaries = exerciseDao.observeProgressSummaries(10).first()
        assertTrue(summaries.isEmpty())
    }
}
