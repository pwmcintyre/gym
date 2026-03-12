package com.gymapp.core.model

import kotlinx.serialization.Serializable

/**
 * One logged set within an [ExerciseEntry].
 * Stored in the `set_entries` Room table.
 *
 * @property id               Stable UUID string primary key.
 * @property exerciseEntryId  FK → [ExerciseEntry.id].
 * @property setNumber        1-based set index within this exercise.
 * @property repsPerformed    Actual reps completed, or null if not recorded.
 * @property weight           Load in kg (or user's chosen unit), null if bodyweight.
 * @property notes            Free-form notes for this set (e.g. "RPE 8", "pause reps").
 */
@Serializable
data class SetEntry(
    val id: String,
    val exerciseEntryId: String,
    val setNumber: Int,
    val repsPerformed: Int?,
    val weight: Float?,
    val notes: String?,
    val weightMode: WeightMode = WeightMode.BARBELL,
)
