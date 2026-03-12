package com.gymapp.core.model

import kotlinx.serialization.Serializable

/**
 * A reusable exercise definition (e.g. "Barbell Back Squat").
 * Stored in the `exercise_templates` Room table.
 *
 * @property id           Stable UUID string primary key.
 * @property name         Human-readable exercise name.
 * @property defaultNotes Optional coaching notes attached to the template.
 */
@Serializable
data class ExerciseTemplate(
    val id: String,
    val name: String,
    val defaultNotes: String?,
)
