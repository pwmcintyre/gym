package com.gymapp.core.model

import kotlinx.serialization.Serializable

/**
 * One exercise within a [WorkoutSession].
 * Stored in the `exercise_entries` Room table.
 *
 * The [TargetPrescription] fields are flattened (embedded) into this table
 * rather than stored as a nested object, so that Room can map them without
 * a type converter.
 *
 * @property id                Stable UUID string primary key.
 * @property workoutSessionId  FK → [WorkoutSession.id].
 * @property label             Ordering label on the workout card (e.g. "A1").
 * @property exerciseName      Name of the exercise as it appears on the card.
 * @property targetSets        Prescribed sets (from [TargetPrescription.sets]).
 * @property targetReps        Prescribed reps (from [TargetPrescription.reps]).
 * @property targetModifier    Rep modifier (from [TargetPrescription.modifier]).
 * @property targetRawText     Raw prescription text (from [TargetPrescription.rawText]).
 * @property notes             Free-form notes for this exercise entry.
 */
@Serializable
data class ExerciseEntry(
    val id: String,
    val workoutSessionId: String,
    val label: String,
    val exerciseName: String,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetModifier: RepModifier,
    val targetRawText: String?,
    val notes: String?,
) {
    /** Convenience accessor reconstructing the prescription value object. */
    val targetPrescription: TargetPrescription
        get() = TargetPrescription(
            sets = targetSets,
            reps = targetReps,
            modifier = targetModifier,
            rawText = targetRawText,
        )
}
