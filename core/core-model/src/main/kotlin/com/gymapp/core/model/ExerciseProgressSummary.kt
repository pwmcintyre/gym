package com.gymapp.core.model

data class ExerciseProgressSummary(
    val exerciseName: String,
    val sessionCount: Int,
    val lastPerformed: Long,
    val bestWeight: Float?,
    val totalVolume: Float,
)
