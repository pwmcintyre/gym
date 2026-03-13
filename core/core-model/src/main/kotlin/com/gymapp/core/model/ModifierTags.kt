package com.gymapp.core.model

val AVAILABLE_MODIFIER_TAGS = listOf(
    "Pause",
    "Tempo",
    "Isometric",
    "Bodyweight",
    "Banded",
)

fun extractModifierTags(text: String?): List<String> {
    val source = text?.lowercase().orEmpty()
    if (source.isBlank()) return emptyList()

    return AVAILABLE_MODIFIER_TAGS.filter { tag ->
        when (tag) {
            "Pause" -> "pause" in source || "paused" in source
            "Tempo" -> "tempo" in source
            "Isometric" -> "isometric" in source || "iso" in source
            "Bodyweight" -> "bodyweight" in source || "pull up" in source || "bw" in source
            "Banded" -> "band" in source || "trx" in source
            else -> false
        }
    }
}
