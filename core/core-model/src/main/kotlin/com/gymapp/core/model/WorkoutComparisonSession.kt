package com.gymapp.core.model

/** Best weight achieved for one exercise within one session of a named workout group. */
data class ExerciseWorkoutProgression(
    val exerciseName: String,
    val sessionId: String,
    val sessionDate: Long,
    val bestWeight: Float,
)
