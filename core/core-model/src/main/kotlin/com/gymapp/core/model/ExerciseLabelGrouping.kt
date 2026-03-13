package com.gymapp.core.model

data class ExerciseLabelParts(
    val supersetKey: String?,
    val memberLabel: String?,
)

data class ExerciseSection(
    val supersetKey: String?,
    val exercises: List<ExerciseEntry>,
)

fun ExerciseEntry.labelParts(): ExerciseLabelParts {
    val trimmed = label.trim()
    if (trimmed.length < 2) return ExerciseLabelParts(supersetKey = null, memberLabel = trimmed.ifBlank { null })

    val prefix = trimmed.takeWhile { it.isLetter() }
    val suffix = trimmed.drop(prefix.length)
    if (prefix.length != 1 || suffix.isBlank() || suffix.any { !it.isDigit() }) {
        return ExerciseLabelParts(supersetKey = null, memberLabel = trimmed.ifBlank { null })
    }

    return ExerciseLabelParts(
        supersetKey = prefix.uppercase(),
        memberLabel = suffix,
    )
}

fun buildExerciseSections(exercises: List<ExerciseEntry>): List<ExerciseSection> {
    if (exercises.isEmpty()) return emptyList()

    val sections = mutableListOf<ExerciseSection>()
    var currentKey: String? = null
    val currentItems = mutableListOf<ExerciseEntry>()

    fun flushSection() {
        if (currentItems.isEmpty()) return
        val useSuperset = currentKey != null && currentItems.size > 1
        sections += ExerciseSection(
            supersetKey = currentKey.takeIf { useSuperset },
            exercises = currentItems.toList(),
        )
        currentItems.clear()
    }

    exercises.forEach { exercise ->
        val nextKey = exercise.labelParts().supersetKey
        if (currentItems.isEmpty()) {
            currentKey = nextKey
            currentItems += exercise
        } else if (currentKey == nextKey && nextKey != null) {
            currentItems += exercise
        } else {
            flushSection()
            currentKey = nextKey
            currentItems += exercise
        }
    }
    flushSection()

    return sections
}
