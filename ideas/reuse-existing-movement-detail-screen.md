# Reuse the existing movement detail screen

Status: backlog idea

Reason:
- The movement detail screen reached from the workout/history flow already feels strong and polished.
- The top-level Movements flow should not create a different detail experience for the same concept.

Scope:
- Remove or avoid any separate movement-detail page introduced from the Movements list.
- Route movement taps to the same detail/progress screen already used from workout/history drill-down.
- Keep movement-detail layout, actions, and data presentation consistent regardless of where the user entered from.
- In workout detail rows, replace the left-side chart/progress affordance with the same movement icon used in the bottom navigation so the action reads as "open this movement".
- Keep charts, history, and related analytics inside the movement detail screen rather than hinting that the workout row itself goes straight to a chart-only view.

Implementation notes:
- Treat `ExerciseProgressScreen` as the single source of truth for movement details unless there is a very strong reason to replace it everywhere.
- If the Movements tab needs extra context, pass that into the existing screen rather than forking the UI.
- The current workout-detail row action in `WorkoutDetailScreen` uses `Icons.Default.BarChart`; swap that affordance to the same `DirectionsRun` glyph used by `BottomNavDestination.Movements`.
- Likely touchpoints: `features/feature-history/src/main/kotlin/com/gymapp/feature/history/WorkoutDetailScreen.kt`, `app/src/main/kotlin/com/gymapp/navigation/BottomNavDestination.kt`, `features/feature-history/src/main/kotlin/com/gymapp/feature/history/MovementsScreen.kt`, `app/src/main/kotlin/com/gymapp/GymAppRoot.kt`, `features/feature-history/src/main/kotlin/com/gymapp/feature/history/ExerciseProgressScreen.kt`.
