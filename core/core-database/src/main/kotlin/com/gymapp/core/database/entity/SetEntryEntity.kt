package com.gymapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exercise_entry_id"]),
    ],
)
data class SetEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "exercise_entry_id")
    val exerciseEntryId: String,

    @ColumnInfo(name = "set_number")
    val setNumber: Int,

    @ColumnInfo(name = "reps_performed")
    val repsPerformed: Int?,

    @ColumnInfo(name = "weight")
    val weight: Float?,

    @ColumnInfo(name = "notes")
    val notes: String?,
)
