package com.gymapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val GymBackground = Color(0xFF0F1115)
val GymSurface = Color(0xFF171A21)
val GymSurfaceVariant = Color(0xFF222733)
val GymPrimary = Color(0xFF7CFF6B)
val GymPrimaryContainer = Color(0xFF1E3A1A)
val GymSecondary = Color(0xFF7AA2FF)
val GymSecondaryContainer = Color(0xFF1B2A45)
val GymTertiary = Color(0xFFFFC857)
val GymTertiaryContainer = Color(0xFF4A3910)
val GymError = Color(0xFFFF6B6B)
val GymErrorContainer = Color(0xFF4B1F24)
val GymOnDark = Color(0xFFF5F7FA)
val GymOnSurfaceVariant = Color(0xFFB9C2D0)
val GymOutline = Color(0xFF394050)
val GymOutlineVariant = Color(0xFF2A303D)
val GymCompleted = Color(0xFF59B76A)
val GymScrim = Color(0xCC090C11)

internal val GymDarkColorScheme = darkColorScheme(
    primary = GymPrimary,
    onPrimary = GymBackground,
    primaryContainer = GymPrimaryContainer,
    onPrimaryContainer = Color(0xFFD6FFD0),
    secondary = GymSecondary,
    onSecondary = GymBackground,
    secondaryContainer = GymSecondaryContainer,
    onSecondaryContainer = GymOnDark,
    tertiary = GymTertiary,
    onTertiary = GymBackground,
    tertiaryContainer = GymTertiaryContainer,
    onTertiaryContainer = GymOnDark,
    error = GymError,
    onError = GymBackground,
    errorContainer = GymErrorContainer,
    onErrorContainer = Color(0xFFFFDAD8),
    background = GymBackground,
    onBackground = GymOnDark,
    surface = GymSurface,
    onSurface = GymOnDark,
    surfaceVariant = GymSurfaceVariant,
    onSurfaceVariant = GymOnSurfaceVariant,
    outline = GymOutline,
    outlineVariant = GymOutlineVariant,
    scrim = GymScrim,
)
