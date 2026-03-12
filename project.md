# Gym Workout Logging App — Product & Engineering Plan

## Overview

A **local-first Android app** for logging gym workouts and automatically generating workout templates by scanning a gym whiteboard.

The app is optimized for **personal use on a Pixel phone** and prioritizes:

* Fast logging during workouts
* Offline operation
* Durable local storage
* AI-assisted workout extraction from photos

---

# Core Goals

1. **Quick workout logging**
2. **Durable local storage**
3. **Offline-first functionality**
4. **Automatic workout extraction from gym whiteboards**
5. **Minimal infrastructure**

---

# Technical Stack

## Mobile App

| Component     | Technology                      |
| ------------- | ------------------------------- |
| Language      | Kotlin                          |
| UI            | Jetpack Compose + Material3     |
| Architecture  | MVVM + Unidirectional Data Flow |
| Local Storage | Room (SQLite)                   |
| Camera        | CameraX                         |
| DI            | Hilt                            |
| Prefs         | Jetpack DataStore               |

## AI Recognition

Whiteboard parsing uses **OpenAI Vision (gpt-4o)** called directly from the app. The user supplies their own OpenAI API key via the Settings screen; it is stored locally in DataStore and never leaves the device except in requests to OpenAI.

No proxy or backend server is required.

---

# Architecture Overview

```
Android App
    │
    ├── Room Database (source of truth)
    │
    ├── DataStore (API key, settings)
    │
    ├── CameraX (photo capture)
    │
    └── OpenAI Vision API (direct HTTPS call)
```

---

# Core Data Model (ADR-0001)

Exercises are modelled in three distinct layers, per ADR-0001.

## Layer 1 — Movement Identity

The canonical exercise, independent of how it is programmed or modified.

```kotlin
Movement
- id
- canonicalName       // "Back Squat"
- aliases             // ["BS", "high bar squat"]
- equipment           // "barbell"
- primaryMuscles      // ["quads", "glutes"]
```

Movement is the **primary grouping key** for analytics and history.

## Layer 2 — Modifiers (Variants)

Variations applied to a movement that produce a distinct variant.

```kotlin
sealed class ExerciseModifier {
    Pause(seconds: Int)
    Tempo(eccentric: Int, isoPause: Int, concentric: Int)
    Equipment(name: String)
    Stance(name: String)
}
```

Examples: `Back Squat` → `Pause Back Squat`, `High Bar Back Squat`

## Layer 3 — Workout Prescription

How the movement is programmed on a given day.

```kotlin
ExerciseEntry                    // = ExerciseInstance
- id
- workoutSessionId
- label                          // "A1", "B2" — ordering on the card
- movementId                     // FK → Movement
- movementName                   // denormalized for display
- modifiers: List<ExerciseModifier>
- targetSets: Int?
- targetReps: Int?
- targetModifier: RepModifier    // NONE | MAX (AMRAP)
- targetRawText: String?
- notes: String?
```

Prescription (sets/reps/weight) **does not affect movement identity**.

## WorkoutSession

```kotlin
WorkoutSession
- id
- date
- notes
- createdAt
```

## SetEntry

Actual performed set.

```kotlin
SetEntry
- id
- exerciseEntryId
- setNumber
- repsPerformed
- weight
- notes
```

## RepModifier

```kotlin
enum class RepModifier { NONE, MAX }
```

| Source Text | Result                   |
| ----------- | ------------------------ |
| `3-4 reps`  | reps = 4                 |
| `MAX`       | modifier = MAX           |
| `4 x MAX`   | sets = 4, modifier = MAX |

---

# AI Output JSON Schema

Current schema returned by OpenAI and parsed by the app:

```json
{
  "workout_date": null,
  "items": [
    {
      "movement": "Back Squat",
      "modifiers": [{"type": "pause", "seconds": 3}],
      "target_sets": 4,
      "target_reps": 8,
      "rep_modifier": "NONE",
      "notes": "",
      "raw_source_text": "Back Squat 4 x 8 3s pause"
    }
  ]
}
```

Supported modifier types: `pause`, `tempo`, `equipment`, `stance`.

All original text preserved in `raw_source_text`.

---

# App Modules

```
app/

core/
    core-model/       Movement, ExerciseEntry, ExerciseModifier, SetEntry, …
    core-database/    Room entities, DAOs, repositories
    core-ai/          OpenAI parser, AiSettings (DataStore)
    core-camera/      CameraX helpers

features/
    feature-workouts/ Manual workout logging (create, add exercises, log sets)
    feature-history/  Past sessions (list + detail + delete)
    feature-scan/     Camera/gallery → AI parse → review → start workout
    feature-settings/ OpenAI API key entry
```

---

# Milestones

## ✅ Milestone 1 — App Foundation

Working Android app with persistent storage, navigation shell, Room DB, camera permissions.

## ✅ Milestone 2 — Manual Workout Logging

Full manual logging: create session, add exercises with targets, log sets (weight + reps), view history, delete sessions.

## ✅ Milestone 3 — AI Whiteboard Scan

Camera capture + gallery image picker → direct OpenAI gpt-4o call → review/edit screen → start workout. User supplies their own API key in Settings.

## ✅ Milestone 3.5 — ADR-0001 Data Model

Restructured exercise data into three layers (Movement Identity / Modifiers / Prescription) per ADR-0001. Added `Movement` entity with `findOrCreate` normalization, `ExerciseModifier` sealed class, updated AI parser to extract movement and modifiers separately.

## ✅ Milestone 3.6 — Theme and Visual Identity

Applied the dark-first "Stealth Training" theme from ADR-0003 across the app shell and core workout flows.

Delivered in this milestone:
- Fixed dark Material 3 palette with neon green accent and custom typography tokens.
- Themed app shell, workout/history screens, scan flow, and settings surfaces around graphite cards and restrained accent usage.
- Updated bottom navigation icon behavior to use outline icons when idle and filled icons when selected.
- Added a new adaptive launcher icon based on the barbell-plate concept from the theme ADR.

Validation note:
- `./gradlew assembleDebug --no-daemon` passed.
- Device install/launch smoke should be rerun on the next attached phone; no device was connected during this session.

---

# User Feedback Fixes (2026-03-13)

Three feedback items addressed:

1. **Keyboard hides last items** — Added `.imePadding()` to `ActiveWorkoutScreen` `LazyColumn` modifier so content scrolls above the software keyboard.
2. **Body weight as configurable weight value** — New `UserSettings` DataStore class stores a user-defined body weight. Settings screen now has a "Body Weight" section. A `BW` chip appears under the weight field in every set row when body weight is configured; tapping it fills in the stored value.
3. **Exercise naming consistency** — `ExerciseEntryDao` now has `observeDistinctNames()`. The Add Exercise dialog shows `SuggestionChip` rows filtered to names containing the typed prefix, letting users reuse prior exercise names exactly.

Deferred: exercise variation management, workouts/history consolidation, banded bodyweight modifier, AI voice input.

---

# Upcoming Milestones

## Milestone 4 — Progress Tracking

Progress charts per movement. PR tracking. Volume trends.

Current direction:
- Start with a Room-backed history overview that summarizes recently trained exercises using session count, best logged weight, total volume, and last-performed date.
- Use the current `exerciseName` field as the grouping key for the first increment, then tighten this to canonical movement identity in a follow-up once that path is wired end-to-end.

Current status:
- Implemented: a `Progress Overview` card on the History screen backed by aggregate Room queries, plus a tap-through detail screen showing session-by-session stats and a simple recent-volume chart for each exercise.
- Remaining: clearer PR-specific views and richer charting if needed.

## ✅ Milestone 5 — Workout Suggestions

Delivered:
- **Template reuse** — each session card in the Workouts list now has a copy icon. Tapping it creates a new session pre-populated with the same exercises (name, label, targets). User lands directly in the new workout.
- **Last-performance hints** — each ExerciseCard shows a `↑ 80kg×5  80kg×5  85kg×3` line sourced from the most recent prior session for that exercise name. Loaded reactively in `ActiveWorkoutViewModel.lastPerformance`.

Validation: `./gradlew assembleDebug --no-daemon` passed.

## ✅ Milestone 6 — Exercise Variation Management

Delivered:
- **Rename exercise** — pencil icon on each ExerciseCard header opens a `RenameExerciseDialog` with the same autocomplete suggestion chips as the Add dialog. Wired to `WorkoutRepository.updateExercise`.
- **Delete exercise** — trash icon opens a confirmation dialog; cascades to sets via DB foreign key.
- Both actions exposed via `ActiveWorkoutViewModel.renameExercise` / `removeExercise`.

## ✅ Milestone 7 — Workout / History Consolidation

Delivered:
- Removed the "History" bottom nav tab entirely.
- `WorkoutsScreen` now shows the Progress Overview card (exercise summaries) and all sessions in one unified list.
- Session card tap → `WorkoutDetailScreen` (read-only history view); copy icon → `ActiveWorkoutScreen` (new editable session); FAB → `ActiveWorkoutScreen` (new blank session).
- `WorkoutsViewModel` now exposes `progressSummaries` alongside `sessions`.
- History routes (`history/detail/{sessionId}`, `history/progress/{exerciseName}`) remain in the nav graph, reachable from the Workouts tab.
- `HistoryScreen` still exists in the module but is no longer used as a top-level tab — it can be deleted in a cleanup pass.

## ✅ Milestone 8 — Banded Bodyweight Support

Delivered:
- New `WeightMode` enum (`BARBELL`, `BODYWEIGHT`, `BANDED`) added to `core-model`.
- `SetEntry` and `SetEntryEntity` now carry `weightMode` (Room type-converted; DB version bumped to 2 with destructive fallback).
- `SetRow` now shows a compact chip row (`BB | BW | Band`) above each set's fields.
  - `BB` mode: normal weight field.
  - `BW` mode: auto-fills from stored body weight, field is read-only.
  - `Band` mode: weight field clears and shows "band" placeholder; value stores band resistance.
- `Add set` copies both weight and mode from the previous set in the exercise.

## Milestone 9 — Cross-device Sync / Backup (was M6)

Google Drive backup (appDataFolder). Optional restore.

Current direction:
- Store workout state as a versioned JSON snapshot of Room-backed workout tables.
- Upload that snapshot to Google Drive `appDataFolder`, not Google Sheets.
- Restore replaces local workout data atomically.
- OpenAI API keys remain local and are excluded from backup.

Current implementation target:
- Settings screen can connect a Google account for Drive backup.
- User can trigger `Back Up Now` and `Restore Backup` manually.
- Backup uses a single JSON snapshot file in Drive `appDataFolder`.
- Restore is explicit and overwrite-based.

Current status:
- Implemented: local snapshot export/import, Google sign-in flow, and manual backup/restore controls in Settings.
- Verified: app builds, installs, and launches on device with the new backup UI.
- Remaining: complete end-to-end Drive round-trip verification after Android OAuth + Drive API setup is configured in Google Cloud.

Setup prerequisite:
- Enable Google Drive API in the Google Cloud project used for the Android app.
- Register an Android OAuth client for package `com.gymapp` with the debug SHA-1 used on this machine/device.
- Until that is configured, the UI can compile and launch but Google sign-in may fail at runtime.

---

# Key Design Decisions

| Decision                  | Reason                                          |
| ------------------------- | ----------------------------------------------- |
| Native Android / Compose  | Best performance, modern UI                     |
| Room DB                   | Reliable offline storage                        |
| Direct OpenAI call        | No backend to host; user controls their own key |
| Three-layer exercise model | Stable movement identity across programs (ADR-0001) |
| Dark-first fixed theme    | High contrast in gym lighting; consistent brand identity |
| DataStore for API key     | Encrypted-by-default local storage              |
| Drive appDataFolder backup | Hidden app-specific storage; snapshot-friendly; avoids Sheets-as-database |
| No proxy server           | Personal use; complexity not justified          |

Current POC note:
- Room currently uses destructive fallback for missing migrations and downgrades so schema churn does not block device testing. Local workout data may be reset after incompatible builds.

---

# Non-Functional Requirements

| Requirement     | Target     |
| --------------- | ---------- |
| App startup     | <2 seconds |
| Save workout    | instant    |
| Scan processing | <10 seconds (OpenAI latency) |

---

# UX Requirements

* Large tap targets
* Minimal typing during workouts
* Fast weight entry
* Clear set progression display
* Scan editing easier than retyping
