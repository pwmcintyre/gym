package com.gymapp.core.database.converter

import androidx.room.TypeConverter
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.WeightMode

/**
 * Room type converters for custom types that cannot be stored natively in SQLite.
 */
class Converters {

    @TypeConverter
    fun fromRepModifier(value: RepModifier): String = value.name

    @TypeConverter
    fun toRepModifier(value: String): RepModifier = RepModifier.valueOf(value)

    @TypeConverter
    fun fromWeightMode(value: WeightMode): String = value.name

    @TypeConverter
    fun toWeightMode(value: String): WeightMode = WeightMode.valueOf(value)
}
