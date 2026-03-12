# ADR-0003: App Theme, Colors, and Iconography

## Status
Accepted

## Context

The workout tracking app needs a consistent visual identity that:

- Works well in **gym lighting conditions** (often dim or high contrast)
- Feels **modern, clean, and focused**
- Aligns with **Android platform conventions**
- Is simple enough for an **AI coding agent to implement reliably**
- Scales well as the app grows

The UI must also integrate smoothly with **Jetpack Compose** and **Material 3**, which are the modern Android UI frameworks.

Design complexity should remain low to ensure the coding agent can implement and maintain the theme without introducing unnecessary styling bugs.

## Decision

The application will use a **dark-first Material 3 theme** with a **neon green accent** and a **minimalist strength-training visual identity**.

### Theme Style

The visual direction is **"Stealth Training"**:

- Dark, graphite-based UI
- High contrast for readability
- Single vivid accent color
- Minimal visual noise
- Numeric data (sets/reps/weight) emphasized

The goal is a **premium, focused, athletic** look rather than a loud "gym bro" aesthetic.

### Design System

The UI will use:

- **Material 3**
- **Jetpack Compose theming**
- **Role-based color tokens** (primary, surface, background, etc.)
- Optional support for **Android dynamic color**

Dynamic color support may be added later but the **default theme will remain fixed** to preserve brand identity.

### Color Palette

Primary palette:

| Role | Color |
|-----|------|
| Background | `#0F1115` |
| Surface | `#171A21` |
| Surface Variant | `#222733` |
| Primary Accent | `#7CFF6B` |
| Primary Container | `#1E3A1A` |
| Secondary | `#7AA2FF` |
| Tertiary | `#FFC857` |
| Error | `#FF6B6B` |
| On Dark Text | `#F5F7FA` |

Usage rules:

- **Primary accent (green)** highlights active actions and selections.
- **Secondary blue** is used sparingly for secondary UI elements.
- **Red is reserved for errors only.**

### UI Semantics

Color meanings:

| Meaning | Color |
|------|------|
| Active Set | Primary accent |
| Completed Set | Muted green |
| OCR Uncertainty | Yellow |
| Error | Red |

### Iconography

The application will use **Material Icons (rounded style)** for in-app UI.

Rules:

- Default icons are **outline style**
- Selected states may use **filled icons**
- Icons remain **single-color** using theme colors
- Avoid mixing icon styles

### Launcher Icon

The launcher icon will follow Android **adaptive icon** guidelines.

Concept:

**Minimalist barbell plate**

Design characteristics:

- Simple circular plate
- Center hole
- One small notch detail
- Strong silhouette
- Works well in monochrome

Rationale:

- Recognizable at small sizes
- Distinct from generic dumbbell icons
- Compatible with Android themed icons

### UX Principles

The UI should emphasize:

- **Speed of logging workouts**
- **Large readable numbers**
- **Minimal distraction**
- **High contrast**

The interface should feel closer to a **training instrument** than a social fitness app.

## Consequences

### Positive

- Consistent visual identity
- Easy implementation using Material 3 defaults
- High readability in gym environments
- Minimal design complexity
- Compatible with Android UI guidelines
- Scales well as the app grows

### Negative

- Less visually playful than some consumer fitness apps
- Fixed theme limits some personalization
- Neon accent may not appeal to all users

### Mitigations

Future versions may add:

- Optional **dynamic color mode**
- Alternative accent palettes
- User-selectable themes

However the **default theme remains the canonical experience**.

## Implementation Notes

The theme should be implemented in:
ui/theme/
Color.kt
Theme.kt
Type.kt


Using Compose Material 3:

- `MaterialTheme`
- `colorScheme`
- `Typography`

All UI components should reference **theme tokens** rather than hardcoded colors.

## Future Work

Possible enhancements:

- Dynamic color toggle
- Theme-aware charts
- Accessibility contrast validation
- AMOLED power-saving mode
