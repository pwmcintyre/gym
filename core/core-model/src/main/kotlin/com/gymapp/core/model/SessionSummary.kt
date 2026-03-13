package com.gymapp.core.model

/** Lightweight per-session stats shown on session cards in the workout list. */
data class SessionSummary(
    val sessionId: String,
    val exerciseCount: Int,
    val setCount: Int,
)
