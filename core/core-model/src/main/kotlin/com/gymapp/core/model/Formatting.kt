package com.gymapp.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared formatting helpers used across all feature modules.
 *
 * These are pure Kotlin functions with no Compose or Android-framework dependency
 * (only java.text / java.util) so they can live in core-model and be imported
 * wherever needed without introducing a UI layer dependency.
 */

/**
 * Formats a weight value (in kg) for display, stripping the trailing ".0" for whole numbers.
 *
 * Examples:
 *   80.0f  → "80 kg"
 *   82.5f  → "82.5 kg"
 *
 * @param appendUnit if true (default) appends " kg"; pass false when the caller
 *                   adds a different suffix (e.g. "Band 20").
 */
fun formatWeight(weight: Float, appendUnit: Boolean = true): String {
    val value = if (weight == weight.toLong().toFloat()) {
        weight.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", weight)
    }
    return if (appendUnit) "$value kg" else value
}

/**
 * Formats a volume value (kg × reps sum) for display. Identical formatting
 * rules to [formatWeight].
 */
fun formatVolume(volume: Float): String = formatWeight(volume)

/**
 * Formats an epoch-millis timestamp as a human-readable date string.
 *
 * Default pattern: "EEE, MMM d yyyy"  →  "Mon, Jan 6 2025"
 */
fun formatDate(epochMillis: Long, pattern: String = "EEE, MMM d yyyy"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMillis))
