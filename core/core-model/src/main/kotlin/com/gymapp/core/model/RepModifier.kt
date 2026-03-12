package com.gymapp.core.model

/**
 * Modifier applied to a rep target, e.g. "3x MAX" means do as many reps as possible.
 */
enum class RepModifier {
    /** No special modifier — reps target is an exact number. */
    NONE,

    /** Perform as many repetitions as possible (AMRAP). */
    MAX,
}
