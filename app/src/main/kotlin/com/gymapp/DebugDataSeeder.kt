package com.gymapp

import com.gymapp.core.database.repository.SetRepository
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.WeightMode
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds realistic sample workout data on first launch (debug builds only).
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

        seedTriphasicRoutine(::daysAgo)
    }

    private suspend fun seedTriphasicRoutine(daysAgo: (Int) -> Long) {
        val weeks = listOf(
            TriphasicWeekSpec(weekNumber = 1, mondayDaysAgo = 27, progressionReps = 8),
            TriphasicWeekSpec(weekNumber = 2, mondayDaysAgo = 20, progressionReps = 8),
            TriphasicWeekSpec(weekNumber = 3, mondayDaysAgo = 13, progressionReps = 6),
            TriphasicWeekSpec(weekNumber = 4, mondayDaysAgo = 6, progressionReps = 4),
        )

        weeks.forEach { week ->
            seedPosteriorChain(daysAgo(week.mondayDaysAgo), week)
            seedChest(daysAgo(week.mondayDaysAgo - 1), week)
            seedWednesday(daysAgo(week.mondayDaysAgo - 2), week)
            seedLegs(daysAgo(week.mondayDaysAgo - 3), week)
            seedShoulders(daysAgo(week.mondayDaysAgo - 4), week)
        }
    }

    private suspend fun seedPosteriorChain(date: Long, week: TriphasicWeekSpec) {
        seedSession(date, "Week ${week.weekNumber} - Posterior Chain") {
            ex("A1", "Barbell Hip Thrusts", 4, week.progressionReps) {
                repeatedSets(weight = 110f + (week.weekNumber - 1) * 10f, count = 4, reps = week.progressionReps)
            }
            ex(
                label = "B1",
                name = "Paused Barbell Deadlift (Isometric)",
                targetSets = 4,
                targetReps = 3,
                modifierTags = listOf("Pause", "Isometric"),
                notes = "2-sec pause above the knee on the way up; 2-sec pause above the knee on the way down",
            ) {
                repeatedSets(weight = 90f + (week.weekNumber - 1) * 7.5f, count = 4, reps = 3)
            }
            ex("C1", "DB Lateral Step Ups", 4, 12, notes = "12 each leg") {
                repeatedSets(weight = 18f + (week.weekNumber - 1) * 2f, count = 4, reps = 12, lastReps = 10)
            }
            ex(
                label = "C2",
                name = "Paused DB Goblet Squat with Glute Band (HEAVY)",
                targetSets = 4,
                targetReps = 10,
                modifierTags = listOf("Pause", "Banded"),
                notes = "3-sec pause at the bottom of the squat",
            ) {
                repeatedSets(weight = 24f + (week.weekNumber - 1) * 2f, count = 4, reps = 10, lastReps = 8)
            }
            ex("D1", "Weighted Plank", 4, null, RepModifier.MAX, modifierTags = listOf("Isometric"), notes = "Max hold") {
                repeatedSets(weight = 15f + (week.weekNumber - 1) * 5f, count = 4, reps = 45 + (week.weekNumber - 1) * 5)
            }
        }
    }

    private suspend fun seedChest(date: Long, week: TriphasicWeekSpec) {
        seedSession(date, "Week ${week.weekNumber} - Chest") {
            ex("A1", "Incline Barbell Bench Press (2-up on incline)", 4, week.progressionReps) {
                repeatedSets(weight = 55f + (week.weekNumber - 1) * 5f, count = 4, reps = week.progressionReps)
            }
            ex(
                label = "B1",
                name = "Paused DB Bench Press (Flat)",
                targetSets = 4,
                targetReps = 8,
                modifierTags = listOf("Pause"),
                notes = "3-sec pause at the bottom; target range 6-8",
            ) {
                repeatedSets(weight = 24f + (week.weekNumber - 1) * 2f, count = 4, reps = 8, lastReps = 6)
            }
            ex(
                label = "C1",
                name = "Paused Pull Ups",
                targetSets = 4,
                targetReps = 6,
                modifierTags = listOf("Pause", "Bodyweight"),
                notes = "2-sec pause when chin passes the bar; alternative: Paused TRX Row 8-10",
            ) {
                bodyweightSets(SEEDED_BODY_WEIGHT_KG, 6 + week.weekNumber, 5 + week.weekNumber, 4 + week.weekNumber, 4 + week.weekNumber)
            }
            ex("C2", "Banded Face Pulls", 4, 15, modifierTags = listOf("Banded")) {
                bandedSets(15, 15, 14, 14)
            }
            ex("D1", "Swiss Ball Jackknifes", 4, 12) {
                repeatedSets(weight = 0f, count = 4, reps = 12, mode = WeightMode.BODYWEIGHT, bodyWeightKg = SEEDED_BODY_WEIGHT_KG)
            }
            ex("D2", "Hollow Hold", 4, null, RepModifier.MAX, modifierTags = listOf("Bodyweight", "Isometric"), notes = "Max hold") {
                bodyweightSets(SEEDED_BODY_WEIGHT_KG, 30 + week.weekNumber * 2, 28 + week.weekNumber * 2, 26 + week.weekNumber * 2, 24 + week.weekNumber * 2)
            }
        }
    }

    private suspend fun seedWednesday(date: Long, week: TriphasicWeekSpec) {
        seedSession(date, "Week ${week.weekNumber} - Wednesday") {
            ex(
                label = "A1",
                name = "BB Curl (Iso Hold + Max Reps)",
                targetSets = 4,
                targetReps = null,
                targetModifier = RepModifier.MAX,
                modifierTags = listOf("Isometric"),
                notes = "20-30s iso hold into max reps (aim for 10+)",
            ) {
                repeatedSets(weight = 20f + (week.weekNumber - 1) * 2.5f, count = 4, reps = 12, lastReps = 10)
            }
            ex(
                label = "A2",
                name = "DB California Press + Neutral Press",
                targetSets = 4,
                targetReps = 8,
                notes = "6-8 California press into max neutral press",
            ) {
                repeatedSets(weight = 14f + (week.weekNumber - 1) * 2f, count = 4, reps = 8, lastReps = 6)
            }
            ex("C1", "Seated Zottman Curl", 4, 12, notes = "10-12") {
                repeatedSets(weight = 12f + (week.weekNumber - 1), count = 4, reps = 12, lastReps = 10)
            }
            ex("C2", "Banded Tricep Kickbacks", 4, 15, modifierTags = listOf("Banded")) {
                bandedSets(15, 15, 14, 14)
            }
            ex("D1", "Kneeling Plate Windmill", 4, 10, notes = "10 each side") {
                repeatedSets(weight = 10f, count = 4, reps = 10)
            }
            ex("D2", "DB Straight Arm Plank Pull Through (HEAVY)", 4, 20) {
                repeatedSets(weight = 20f + (week.weekNumber - 1) * 2f, count = 4, reps = 20, lastReps = 18)
            }
            ex("D3", "Seated Tuck Arm Raises (with plates)", 4, 15, notes = "12-15") {
                repeatedSets(weight = 5f + (week.weekNumber - 1), count = 4, reps = 15, lastReps = 12)
            }
        }
    }

    private suspend fun seedLegs(date: Long, week: TriphasicWeekSpec) {
        seedSession(date, "Week ${week.weekNumber} - Legs") {
            ex(
                label = "A1",
                name = "BB Front Squat",
                targetSets = 4,
                targetReps = week.progressionReps,
                notes = "Alternative option: Zercher Squat",
            ) {
                repeatedSets(weight = 70f + (week.weekNumber - 1) * 7.5f, count = 4, reps = week.progressionReps)
            }
            ex(
                label = "B1",
                name = "Paused BB Back Squat",
                targetSets = 4,
                targetReps = 3,
                modifierTags = listOf("Pause"),
                notes = "3-sec pause at the bottom",
            ) {
                repeatedSets(weight = 80f + (week.weekNumber - 1) * 7.5f, count = 4, reps = 3)
            }
            ex("C1", "DB B-Stance Split Squat", 4, 10, notes = "10 each leg") {
                repeatedSets(weight = 20f + (week.weekNumber - 1) * 2f, count = 4, reps = 10, lastReps = 8)
            }
            ex("C2", "KB Goblet Sumo Squat", 4, 12) {
                repeatedSets(weight = 28f + (week.weekNumber - 1) * 4f, count = 4, reps = 12, lastReps = 10)
            }
            ex(
                label = "D1",
                name = "Bulgarian Split Squat Iso Hold (DB or bodyweight)",
                targetSets = 4,
                targetReps = null,
                targetModifier = RepModifier.MAX,
                modifierTags = listOf("Bodyweight", "Isometric"),
                notes = "Max hold each leg",
            ) {
                bodyweightSets(SEEDED_BODY_WEIGHT_KG, 35 + week.weekNumber * 2, 34 + week.weekNumber * 2, 32 + week.weekNumber * 2, 30 + week.weekNumber * 2)
            }
            ex("D2", "Banded Woodchop", 4, 10, modifierTags = listOf("Banded"), notes = "10 each side") {
                bandedSets(10, 10, 10, 10)
            }
        }
    }

    private suspend fun seedShoulders(date: Long, week: TriphasicWeekSpec) {
        seedSession(date, "Week ${week.weekNumber} - Shoulders") {
            ex("A1", "DB Shoulder Press (Seated)", 4, week.progressionReps) {
                repeatedSets(weight = 20f + (week.weekNumber - 1) * 2f, count = 4, reps = week.progressionReps)
            }
            ex(
                label = "B1",
                name = "Paused Standing Single DB Shoulder Press",
                targetSets = 4,
                targetReps = 8,
                modifierTags = listOf("Pause"),
                notes = "2-sec pause at lockout; target range 6-8",
            ) {
                repeatedSets(weight = 16f + (week.weekNumber - 1) * 2f, count = 4, reps = 8, lastReps = 6)
            }
            ex(
                label = "B2",
                name = "Pull Ups (Muscle Endurance)",
                targetSets = 4,
                targetReps = null,
                targetModifier = RepModifier.MAX,
                modifierTags = listOf("Bodyweight"),
                notes = "Use bands, aim for 10",
            ) {
                bodyweightSets(SEEDED_BODY_WEIGHT_KG, 10 + week.weekNumber, 9 + week.weekNumber, 8 + week.weekNumber, 8 + week.weekNumber)
            }
            ex(
                label = "C1",
                name = "Paused Single Arm DB Row",
                targetSets = 3,
                targetReps = 10,
                modifierTags = listOf("Pause"),
                notes = "2-sec pause at the top; target range 8-10",
            ) {
                repeatedSets(weight = 24f + (week.weekNumber - 1) * 2f, count = 3, reps = 10, lastReps = 8)
            }
            ex(
                label = "C2",
                name = "Plate Front Raise + Truck Driver",
                targetSets = 3,
                targetReps = 15,
                notes = "1 rep = raise + rotation; target range 12-15",
            ) {
                repeatedSets(weight = 10f + (week.weekNumber - 1) * 2f, count = 3, reps = 15, lastReps = 12)
            }
            ex("C3", "Prone Plate Windmill Passes", 3, 8, notes = "6-8 each direction") {
                repeatedSets(weight = 5f + (week.weekNumber - 1), count = 3, reps = 8, lastReps = 6)
            }
        }
    }

    private inner class SessionScope(val sessionId: String) {
        suspend fun ex(
            label: String,
            name: String,
            targetSets: Int,
            targetReps: Int?,
            targetModifier: RepModifier = RepModifier.NONE,
            modifierTags: List<String> = emptyList(),
            notes: String? = null,
            targetRawText: String? = null,
            block: suspend ExerciseScope.() -> Unit,
        ) {
            val entry = workoutRepository.addExercise(
                sessionId = sessionId,
                label = label,
                exerciseName = name,
                targetSets = targetSets,
                targetReps = targetReps,
                targetModifier = targetModifier,
                targetRawText = targetRawText,
                modifierTags = modifierTags,
                notes = notes,
            )
            ExerciseScope(entry.id).block()
        }
    }

    private inner class ExerciseScope(val exerciseId: String) {
        private var setNum = 1

        suspend fun repeatedSets(
            weight: Float,
            count: Int,
            reps: Int,
            lastReps: Int = reps,
            mode: WeightMode = WeightMode.BARBELL,
            bodyWeightKg: Float? = null,
        ) {
            repeat(count) { index ->
                val loggedWeight = when (mode) {
                    WeightMode.BODYWEIGHT -> bodyWeightKg
                    WeightMode.BANDED -> null
                    WeightMode.BARBELL -> weight
                }
                val loggedReps = if (index == count - 1) lastReps else reps
                setRepository.addSet(
                    exerciseEntryId = exerciseId,
                    setNumber = setNum++,
                    repsPerformed = loggedReps,
                    weight = loggedWeight,
                    weightMode = mode,
                )
            }
        }

        suspend fun bodyweightSets(bodyWeightKg: Float, vararg repCounts: Int) {
            repCounts.forEach { reps ->
                setRepository.addSet(
                    exerciseEntryId = exerciseId,
                    setNumber = setNum++,
                    repsPerformed = reps,
                    weight = bodyWeightKg,
                    weightMode = WeightMode.BODYWEIGHT,
                )
            }
        }

        suspend fun bandedSets(vararg repCounts: Int) {
            repCounts.forEach { reps ->
                setRepository.addSet(
                    exerciseEntryId = exerciseId,
                    setNumber = setNum++,
                    repsPerformed = reps,
                    weight = null,
                    weightMode = WeightMode.BANDED,
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
        if (!looksLikeOldSeededPplBlock(existingSessions)) return

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

    private fun looksLikeOldSeededPplBlock(sessions: List<com.gymapp.core.model.WorkoutSession>): Boolean {
        if (sessions.size != 15) return false
        val workoutNames = sessions.mapNotNull { it.notes }.toSet()
        return workoutNames == setOf("Legs", "Push", "Pull")
    }

    private data class TriphasicWeekSpec(
        val weekNumber: Int,
        val mondayDaysAgo: Int,
        val progressionReps: Int,
    )

    private companion object {
        const val SEEDED_BODY_WEIGHT_KG = 75f
    }
}
