package com.gymapp.core.model

data class ExerciseSessionProgress(
    val sessionId: String,
    val sessionDate: Long,
    val setCount: Int,
    val bestWeight: Float?,
    val totalVolume: Float,
    val bodyweightSetCount: Int = 0,
)
