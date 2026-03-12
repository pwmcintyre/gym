package com.gymapp.core.database.converter

import androidx.room.TypeConverter
import com.gymapp.core.model.RepModifier

/**
 * Room type converters for custom types that cannot be stored natively in SQLite.
 */
class Converters {

    @TypeConverter
    fun fromRepModifier(value: RepModifier): String = value.name

    @TypeConverter
    fun toRepModifier(value: String): RepModifier = RepModifier.valueOf(value)
}
