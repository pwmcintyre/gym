package com.gymapp

import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.WeightMode
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds realistic sample workout data spanning ~5 weeks on first launch (debug builds only).
 * No-ops if the database already contains sessions.
 */
@Singleton
class DebugDataSeeder @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val setRepository: SetRepository,
) {
    suspend fun seedIfEmpty() {
        val existingSessions = workoutRepository.observeAll().first()
        if (existingSessions.isNotEmpty()) {
            backfillSeededBodyweightSamples(existingSessions)
            return
        }

        val now = System.currentTimeMillis()
        fun daysAgo(days: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -days)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // 5-week PPL block — progressive overload each week
        // Week 5 (oldest)
        seedSession(daysAgo(34), "Legs") {
            ex("Back Squat", 4, 5) { sets(80f, 80f, 82.5f, 82.5f, reps = 5, lastReps = 4) }
            ex("Romanian Deadlift", 3, 10) { sets(60f, 60f, 60f, reps = 10, lastReps = 9) }
            ex("Leg Press", 3, 12) { sets(100f, 100f, 100f, reps = 12, lastReps = 11) }
        }
        seedSession(daysAgo(32), "Push") {
            ex("Bench Press", 4, 8) { sets(60f, 62.5f, 62.5f, 62.5f, reps = 8, lastReps = 6) }
            ex("Overhead Press", 3, 8) { sets(37.5f, 37.5f, 37.5f, reps = 8, lastReps = 7) }
            ex("Tricep Pushdown", 3, 12) { sets(25f, 25f, 25f, reps = 12, lastReps = 10) }
        }
        seedSession(daysAgo(30), "Pull") {
            ex("Deadlift", 3, 5) { sets(100f, 102.5f, 105f, reps = 5, lastReps = 4) }
            ex("Barbell Row", 4, 8) { sets(60f, 60f, 62.5f, 62.5f, reps = 8, lastReps = 7) }
            ex("Pull Up", 3, null, RepModifier.MAX) { bwSets(75f, 8, 7, 6) }
        }

        // Week 4
        seedSession(daysAgo(27), "Legs") {
            ex("Back Squat", 4, 5) { sets(82.5f, 82.5f, 85f, 85f, reps = 5, lastReps = 4) }
            ex("Romanian Deadlift", 3, 10) { sets(62.5f, 62.5f, 62.5f, reps = 10) }
            ex("Leg Press", 3, 12) { sets(100f, 102.5f, 102.5f, reps = 12) }
        }
        seedSession(daysAgo(25), "Push") {
            ex("Bench Press", 4, 8) { sets(62.5f, 62.5f, 65f, 65f, reps = 8, lastReps = 6) }
            ex("Overhead Press", 3, 8) { sets(37.5f, 40f, 40f, reps = 8, lastReps = 6) }
            ex("Dumbbell Fly", 3, 12) { sets(14f, 14f, 14f, reps = 12, lastReps = 11) }
        }
        seedSession(daysAgo(23), "Pull") {
            ex("Deadlift", 3, 5) { sets(105f, 105f, 107.5f, reps = 5, lastReps = 4) }
            ex("Barbell Row", 4, 8) { sets(62.5f, 62.5f, 65f, 65f, reps = 8, lastReps = 7) }
            ex("Pull Up", 3, null, RepModifier.MAX) { bwSets(75f, 9, 8, 7) }
        }

        // Week 3
        seedSession(daysAgo(20), "Legs") {
            ex("Back Squat", 4, 5) { sets(85f, 85f, 85f, 87.5f, reps = 5, lastReps = 4) }
            ex("Romanian Deadlift", 3, 10) { sets(65f, 65f, 65f, reps = 10, lastReps = 9) }
            ex("Leg Press", 3, 12) { sets(105f, 105f, 105f, reps = 12, lastReps = 11) }
        }
        seedSession(daysAgo(18), "Push") {
            ex("Bench Press", 4, 8) { sets(65f, 65f, 65f, 67.5f, reps = 8, lastReps = 6) }
            ex("Overhead Press", 3, 8) { sets(40f, 40f, 40f, reps = 8, lastReps = 7) }
            ex("Tricep Pushdown", 3, 12) { sets(27.5f, 27.5f, 27.5f, reps = 12, lastReps = 10) }
        }
        seedSession(daysAgo(16), "Pull") {
            ex("Deadlift", 3, 5) { sets(107.5f, 110f, 110f, reps = 5, lastReps = 4) }
            ex("Barbell Row", 4, 8) { sets(65f, 65f, 67.5f, 67.5f, reps = 8, lastReps = 7) }
            ex("Pull Up", 3, null, RepModifier.MAX) { bwSets(75f, 10, 8, 7) }
        }

        // Week 2
        seedSession(daysAgo(13), "Legs") {
            ex("Back Squat", 4, 5) { sets(87.5f, 87.5f, 90f, 90f, reps = 5, lastReps = 4) }
            ex("Romanian Deadlift", 3, 10) { sets(65f, 67.5f, 67.5f, reps = 10, lastReps = 9) }
            ex("Leg Press", 3, 12) { sets(107.5f, 107.5f, 110f, reps = 12, lastReps = 11) }
        }
        seedSession(daysAgo(11), "Push") {
            ex("Bench Press", 4, 8) { sets(67.5f, 67.5f, 67.5f, 70f, reps = 8, lastReps = 5) }
            ex("Overhead Press", 3, 8) { sets(40f, 42.5f, 42.5f, reps = 8, lastReps = 6) }
            ex("Dumbbell Fly", 3, 12) { sets(15f, 15f, 15f, reps = 12, lastReps = 10) }
        }
        seedSession(daysAgo(9), "Pull") {
            ex("Deadlift", 3, 5) { sets(110f, 112.5f, 112.5f, reps = 5) }
            ex("Barbell Row", 4, 8) { sets(67.5f, 70f, 70f, 70f, reps = 8, lastReps = 7) }
            ex("Pull Up", 3, null, RepModifier.MAX) { bwSets(75f, 10, 9, 8) }
        }

        // Week 1 (most recent)
        seedSession(daysAgo(6), "Legs") {
            ex("Back Squat", 4, 5) { sets(90f, 90f, 92.5f, 92.5f, reps = 5, lastReps = 4) }
            ex("Romanian Deadlift", 3, 10) { sets(67.5f, 67.5f, 70f, reps = 10, lastReps = 9) }
            ex("Leg Press", 3, 12) { sets(110f, 110f, 112.5f, reps = 12, lastReps = 11) }
        }
        seedSession(daysAgo(4), "Push") {
            ex("Bench Press", 4, 8) { sets(70f, 70f, 70f, 72.5f, reps = 8, lastReps = 5) }
            ex("Overhead Press", 3, 8) { sets(42.5f, 42.5f, 42.5f, reps = 8, lastReps = 7) }
            ex("Tricep Pushdown", 3, 12) { sets(30f, 30f, 30f, reps = 12, lastReps = 10) }
        }
        seedSession(daysAgo(2), "Pull") {
            ex("Deadlift", 3, 5) { sets(112.5f, 115f, 115f, reps = 5, lastReps = 4) }
            ex("Barbell Row", 4, 8) { sets(70f, 70f, 72.5f, 72.5f, reps = 8, lastReps = 7) }
            ex("Pull Up", 3, null, RepModifier.MAX) { bwSets(75f, 11, 9, 8) }
        }
    }

    // ── DSL ──────────────────────────────────────────────────────────────────

    private inner class SessionScope(val sessionId: String) {
        private val labels = listOf("A1", "B1", "C1", "A2", "B2", "C2")
        private var labelIdx = 0

        suspend fun ex(
            name: String,
            targetSets: Int,
            targetReps: Int?,
            targetModifier: RepModifier = RepModifier.NONE,
            block: suspend ExerciseScope.() -> Unit,
        ) {
            val label = labels.getOrElse(labelIdx++) { "Z$labelIdx" }
            val entry = workoutRepository.addExercise(
                sessionId = sessionId,
                label = label,
                exerciseName = name,
                targetSets = targetSets,
                targetReps = targetReps,
                targetModifier = targetModifier,
            )
            ExerciseScope(entry.id).block()
        }
    }

    private inner class ExerciseScope(val exerciseId: String) {
        private var setNum = 1

        /** All sets at the given weights; first N-1 at [reps], last at [lastReps] (defaults to [reps]). */
        suspend fun sets(vararg weights: Float, reps: Int, lastReps: Int = reps) {
            weights.forEachIndexed { i, w ->
                val r = if (i == weights.lastIndex) lastReps else reps
                setRepository.addSet(exerciseId, setNum++, repsPerformed = r, weight = w)
            }
        }

        suspend fun bwSets(bodyWeightKg: Float, vararg repCounts: Int) {
            repCounts.forEach { r ->
                setRepository.addSet(
                    exerciseEntryId = exerciseId,
                    setNumber = setNum++,
                    repsPerformed = r,
                    weight = bodyWeightKg,
                    weightMode = WeightMode.BODYWEIGHT,
                )
            }
        }
    }

    private suspend fun seedSession(
        date: Long,
        name: String,
        block: suspend SessionScope.() -> Unit,
    ) {
        val session = workoutRepository.create(date = date, notes = name)
        SessionScope(session.id).block()
    }

    private suspend fun backfillSeededBodyweightSamples(existingSessions: List<com.gymapp.core.model.WorkoutSession>) {
        if (!looksLikeSeededPplBlock(existingSessions)) return

        existingSessions.forEach { session ->
            val exercises = workoutRepository.observeExercises(session.id).first()
            exercises
                .filter { it.exerciseName == "Pull Up" }
                .forEach { exercise ->
                    val sets = setRepository.observeSets(exercise.id).first()
                    sets
                        .filter { it.weightMode == WeightMode.BODYWEIGHT && it.weight == null }
                        .forEach { set ->
                            setRepository.update(set.copy(weight = SEEDED_BODY_WEIGHT_KG))
                        }
                }
        }
    }

    private fun looksLikeSeededPplBlock(sessions: List<com.gymapp.core.model.WorkoutSession>): Boolean {
        if (sessions.size != 15) return false
        val workoutNames = sessions.mapNotNull { it.notes }.toSet()
        return workoutNames == setOf("Legs", "Push", "Pull")
    }

    private companion object {
        const val SEEDED_BODY_WEIGHT_KG = 75f
    }
}
