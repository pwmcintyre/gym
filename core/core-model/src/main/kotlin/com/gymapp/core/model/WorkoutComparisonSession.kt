package com.gymapp.core.model

/** Aggregated metrics for a single session belonging to a named workout group. */
data class WorkoutComparisonSession(
    val sessionId: String,
    val sessionDate: Long,
    val totalVolume: Float,
)

/** Best weight achieved for one exercise within one session of a named workout group. */
data class ExerciseWorkoutProgression(
    val exerciseName: String,
    val sessionId: String,
    val sessionDate: Long,
    val bestWeight: Float,
)
