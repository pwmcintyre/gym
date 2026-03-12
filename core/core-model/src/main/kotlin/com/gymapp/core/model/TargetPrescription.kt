package com.gymapp.core.model

import kotlinx.serialization.Serializable

/**
 * The prescribed target for an exercise within a session.
 * Stored as an embedded object in [ExerciseEntry].
 *
 * @property sets      Number of sets prescribed, or null if not specified.
 * @property reps      Number of reps per set prescribed, or null if not specified.
 * @property modifier  How to interpret [reps] (e.g. exact vs AMRAP).
 * @property rawText   The original free-form text from the workout card scan (e.g. "3x8-10").
 */
@Serializable
data class TargetPrescription(
    val sets: Int?,
    val reps: Int?,
    val modifier: RepModifier,
    val rawText: String?,
)
