package com.gymapp.core.database.converter

import androidx.room.TypeConverter
import com.gymapp.core.model.RepModifier
import com.gymapp.core.model.WeightMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for custom types that cannot be stored natively in SQLite.
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRepModifier(value: RepModifier): String = value.name

    @TypeConverter
    fun toRepModifier(value: String): RepModifier = RepModifier.valueOf(value)

    @TypeConverter
    fun fromWeightMode(value: WeightMode): String = value.name

    @TypeConverter
    fun toWeightMode(value: String): WeightMode = WeightMode.valueOf(value)

    @TypeConverter
    fun fromStringList(values: List<String>): String = json.encodeToString(values)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf { it.isNotBlank() }?.let { json.decodeFromString(it) } ?: emptyList()
}
