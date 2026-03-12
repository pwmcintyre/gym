package com.gymapp.core.model

import kotlinx.serialization.Serializable

/**
 * A single training session on a given date.
 * Stored in the `workout_sessions` Room table.
 *
 * @property id         Stable UUID string primary key.
 * @property date       Unix epoch millis for the calendar day of the session.
 * @property notes      Free-form session notes.
 * @property createdAt  Unix epoch millis at record creation (for ordering / sync).
 */
@Serializable
data class WorkoutSession(
    val id: String,
    val date: Long,
    val notes: String?,
    val createdAt: Long,
)
