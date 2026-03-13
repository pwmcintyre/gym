package com.gymapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymapp.core.model.RepModifier

@Entity(
    tableName = "exercise_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["workout_session_id"]),
    ],
)
data class ExerciseEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "workout_session_id")
    val workoutSessionId: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    // Flattened TargetPrescription fields
    @ColumnInfo(name = "target_sets")
    val targetSets: Int?,

    @ColumnInfo(name = "target_reps")
    val targetReps: Int?,

    @ColumnInfo(name = "target_modifier")
    val targetModifier: RepModifier,

    @ColumnInfo(name = "target_raw_text")
    val targetRawText: String?,

    @ColumnInfo(name = "modifier_tags")
    val modifierTags: List<String>,

    @ColumnInfo(name = "notes")
    val notes: String?,
)
