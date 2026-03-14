package com.gymapp.core.model

val AVAILABLE_MODIFIER_TAGS = listOf(
    "Pause",
    "Tempo",
    "Iso Hold",
    "Seated",
    "Standing",
    "Single Arm",
    "Incline",
    "Flat",
    "Neutral",
    "B-Stance",
    "Sumo",
    "Kneeling",
    "Prone",
)

fun extractModifierTags(vararg texts: String?): List<String> {
    val source = texts.joinToString(" ").lowercase()
    if (source.isBlank()) return emptyList()

    return AVAILABLE_MODIFIER_TAGS.filter { tag ->
        when (tag) {
            "Pause" -> "pause" in source || "paused" in source
            "Tempo" -> "tempo" in source
            "Iso Hold" -> "iso hold" in source || "isometric" in source
            "Seated" -> "seated" in source
            "Standing" -> "standing" in source
            "Single Arm" -> "single arm" in source || "single-arm" in source
            "Incline" -> "incline" in source
            "Flat" -> "flat" in source
            "Neutral" -> "neutral" in source
            "B-Stance" -> "b-stance" in source || "b stance" in source
            "Sumo" -> "sumo" in source
            "Kneeling" -> "kneeling" in source
            "Prone" -> "prone" in source
            else -> false
        }
    }
}
