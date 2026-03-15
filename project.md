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

## Current UI backlog

- Workouts page is the active UI polish surface.
- Date-grouped workout history, stronger session-state cues, AI action cleanup, and movement-history date grouping are already shipped.
- Workout creation now lives on the workouts page: scan moved out of bottom nav, the floating add button is gone, and a top-of-list hero CTA now handles new, suggested, and scanned workout starts.
- User-facing `exercise` terminology has now been exhausted on the shipped UI; remaining `exercise` references are internal model names, route args, or compatibility-sensitive schema fields.
- Movement detail personal records are now reduced to two headline metrics: top weight and top reps, instead of a dense all-PR grid.
- Whiteboard-style labels such as `A1` and `A2` are now grouped visually as lightweight supersets in active-workout and workout-detail screens, without changing storage or scan parsing.
- Movement trend charts now support point selection with an `Open workout` action, so chart interaction can jump directly to the underlying session instead of acting as a static graphic.
- Workout entries now store real `modifierTags` alongside notes, and the workout editor uses highlighted tag toggles for richer labels such as `Seated`, `Single Arm`, `Incline`, and `Iso Hold` while keeping separate free-form notes. The debug seed routine now normalizes base movement names and uses those labels instead of baking pause/position text into the movement name.
- AI workout suggestions no longer fail silently on empty-screen workout creation: failures now surface an inline error, and the reply parser is slightly more forgiving of numbered or bulleted lines. A unit test now covers the suggestion parser’s strict and loose formats.
- The workouts landing screen now surfaces a `Today's focus` card when there is a session dated today, giving the page a more directed entry state without changing navigation.
- The global coach overlay now carries hidden nav context plus fresh workout data into every request, including recent sessions and movement-specific history when relevant. Scan-response parsing also has dedicated unit coverage for malformed and empty AI payloads.
- Settings now include a dedicated `Training Constraints` editor backed by DataStore, and the coach prompt silently respects those notes on every reply.
- Cold launch now opens the coach overlay once with a short delay and seeds it with a brief recent-training opener, while still falling back to a simple local welcome when no data exists.
- That coach opener now follows a tighter structure: one honest encouraging review sentence, one next-workout suggestion sentence, and a one-tap action that creates a new workout and expands the suggestion into a concrete plan on the workout screen.
- The coach overlay no longer expands to a near full-screen sheet; it now renders as an animated floating panel above the single global coach button, and the older per-screen robot assistant entry points have been removed.
- Coach IME behavior was tightened: opening the keyboard now lifts the overlay upward via IME insets instead of compressing the panel height, and this was re-verified on a connected Pixel-sized device.
- Remaining larger follow-ups still live in `ideas/` and should be handled as separate slices once this creation-flow cleanup lands.

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
3. **Demo bodyweight history no longer renders blank** — Debug seed data now stores `75 kg` as the system weight for sample pull-up sets, and existing seeded debug installs get a targeted backfill for those sample bodyweight sets.
3. **Exercise naming consistency** — `ExerciseEntryDao` now has `observeDistinctNames()`. The Add Exercise dialog shows `SuggestionChip` rows filtered to names containing the typed prefix, letting users reuse prior exercise names exactly.
4. **Debug demo routine refreshed** — The old generic PPL seed block has been replaced with a 4-week `TRIPHASIC BLOCK 2` demo routine covering Posterior Chain, Chest, Wednesday accessories, Legs, and Shoulders with explicit whiteboard labels, bodyweight/banded cases, iso-hold style work, and week-based rep changes.

Deferred: exercise variation management, workouts/history consolidation, banded bodyweight modifier, AI voice input.

---

# Upcoming Milestones

## ✅ Milestone 4 — Progress Tracking (complete)

Progress charts per movement. PR tracking. Volume trends.

Delivered (initial increment — earlier session):
- `Progress Overview` card on the Workouts screen backed by aggregate Room queries.
- Tap-through `ExerciseProgressScreen` showing session-by-session stats and a volume bar chart.

Delivered (this session):
- **Personal Records card** — new `ExercisePr(reps, weight)` model; new `SetEntryDao.observePrsByRepCount()` query (best weight per rep count, all-time); wired through `SetRepository` and `ExerciseProgressViewModel`. Displayed as a grid of `PrChip` tiles on the progress screen (up to 8 PR rows, e.g. 1RM, 2RM, 3RM …).
- **Weight trend line** — `ExerciseProgressChartCard` now draws volume bars (subdued) behind a solid weight-trend line (best weight per session plotted per-bar) on the same canvas. Legend row identifies bars vs line.
- Chart window expanded from 6 → 8 sessions.

Validation: `./gradlew assembleDebug testDebugUnitTest --no-daemon` — BUILD SUCCESSFUL.

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
- Google sign-in now works on device — user can connect their Google account successfully.
- Drive API enabled in Google Cloud Console for project gym-app-70175.
- Remaining blocker: 403 Forbidden on Drive API calls — likely because the Drive API was enabled on the Firebase project's Cloud project, but the OAuth client used by the app may be scoped to the old deleted client or the wrong project. Need to verify which Cloud project the enabled Drive API is in, and that it matches the project ID in google-services.json (project_number: 549580093290).

Next session steps:
1. Verify Google Cloud Console project: confirm Drive API is enabled on the project matching `project_id` in app/google-services.json (should be `gym-app-70175`).
2. Check OAuth consent screen scopes — ensure `https://www.googleapis.com/auth/drive.appdata` scope is listed.
3. Check that the test user (your Google account) is added to the OAuth consent screen.
4. Re-test Back Up Now — expect success.
5. Test Restore Backup round-trip.
6. Mark M9 complete and commit.

Setup notes (completed):
- Firebase Authentication → Google Sign-in enabled → google-services.json now has 2 oauth_client entries.
- SHA-1 fingerprint conflict resolved by deleting the old OAuth client in the other Cloud project.
- google-services.json moved to app/ and build verified (BUILD SUCCESSFUL).
- Drive API enabled in Google Cloud Console.
- Debug SHA-1: F0:33:07:0B:86:EA:DF:5B:87:F5:78:43:FE:DD:CA:14:D9:3A:DD:B4
- Firebase project: gym-app-70175 (project number 549580093290)
- Android OAuth client ID: 549580093290-lubnmlu2p3mp8c8gs2f41eobkevgcga5.apps.googleusercontent.com

## ✅ Milestone 10 — Rest Timer

Auto-starts a configurable countdown after each logged set. Visible inline in the ExerciseCard with a circular progress indicator, MM:SS countdown, and a cancel button. Vibrates on completion.

Delivered:
- `RestTimerState` sealed class (`Idle` / `Running(exerciseId, remainingSeconds, totalSeconds)`) in `ActiveWorkoutViewModel`.
- `startRestTimer(exerciseId)` called from `addSet()`; ticks every second via coroutine; `cancelRestTimer()` public method.
- `restTimerSeconds: Flow<Int>` (default 90s) added to `UserSettings` DataStore and wired into `ActiveWorkoutViewModel`.
- `RestTimerRow` composable in `ActiveWorkoutScreen` — circular progress, MM:SS text, "Rest" label, close button.
- Vibration on expiry (400ms, API-level appropriate).
- Settings screen: new "Rest Timer" card with 60s/90s/120s/180s preset `FilterChip` row. Persists via `SettingsViewModel.saveRestTimerSeconds()`.

Validation: `./gradlew assembleDebug --no-daemon` BUILD SUCCESSFUL.

---

## ✅ Milestone 11 — QoL / Bug Fix Pass

Autonomous session; no user input required. Build and unit tests passed after all changes.

Fixes:
- **`RepModifier.MAX` display bug** — `ActiveWorkoutScreen` and `WorkoutDetailScreen` were printing "MAX" for any exercise with `targetReps == null` regardless of `targetModifier`. Fixed to check `targetModifier == RepModifier.MAX` explicitly.
- **`ScanReviewScreen` keyboard hide** — added `imePadding()` to the `LazyColumn` (same fix previously applied to `ActiveWorkoutScreen`).
- **Focus-loss save for text fields** — `WorkoutNotesField`, API key field, and body weight field now save on focus loss via `onFocusChanged`, not only on Done key.
- **Add set copies reps** — `addSet()` now copies `repsPerformed` from the last set in the exercise, not hardcoded `null`.
- **Gallery image read on main thread** — moved `openInputStream(uri).readBytes()` off the Compose callback into `ScanViewModel.parseUri()` on `Dispatchers.IO`.
- **`remember` key for set fields** — `SetRow` now uses `remember(set.id, set.weight, set.repsPerformed)` so externally changed values reinitialize the local text state.
- **Removed dead code** — `BottomNavDestination.History` object and `Routes.HISTORY` constant (left from M7 consolidation).

Improvements:
- **Session cards show exercise/set count** — `SessionSummary` model + `observeSessionSummaries()` DAO query; cards now display "N exercises · M sets".
- **Cached `observeSets()` StateFlows** — both `ActiveWorkoutViewModel` and `WorkoutDetailViewModel` use a `mutableMapOf` cache to avoid creating new StateFlows on every recomposition.
- **Sticky rest timer banner** — when the rest timer is running and its exercise card is scrolled off-screen, a compact `RestTimerBanner` appears at the top of the content area showing the countdown and a cancel button.

Validation: `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` — BUILD SUCCESSFUL.

## ✅ Milestone 12 — M4 completion + code cleanup

Autonomous session.

Delivered:
- **M4 Personal Records** — see Milestone 4 section above for full detail.
- **M4 Weight trend line** — see Milestone 4 section above.
- **Shared formatting utilities** — new `core/core-model/Formatting.kt` with `formatWeight(Float, appendUnit)`, `formatVolume(Float)`, `formatDate(Long, pattern)`. Removed 5 private copies of `formatWeight` and 3 copies of `formatDate` from `ActiveWorkoutScreen`, `WorkoutDetailScreen`, `WorkoutsScreen`, and `ExerciseProgressScreen`. All callers now import from `com.gymapp.core.model`.
- **Removed `WorkoutSessionDao.observeInRange()`** — dead query with no callers in the entire codebase. (Keeping `ExerciseTemplateDao` — used by the backup module.)

Validation: `./gradlew assembleDebug testDebugUnitTest --no-daemon` — BUILD SUCCESSFUL.

Remaining:
- M9 Drive backup 403 blocker (needs user to verify Cloud Console setup).
- Dead infrastructure: `ExerciseTemplateDao` kept intentionally (used by backup).

---

## ✅ Milestone 13 — AI Assistant (voice + chat)

Autonomous session.

Delivered:
- **`AiAssistant` service** (`core/core-ai`) — thin GPT-4o chat completions wrapper. Takes a system prompt + `List<ChatMessage>` conversation history, returns `Result<String>`. Shares the `OkHttpClient` + serialization pattern from `ProxyWorkoutCardParser`.
- **`AiAssistantViewModel`** (`feature-workouts`) — `AndroidViewModel` managing:
  - Conversation history (bubbles shown in the sheet).
  - Mic state (`Idle` / `Listening` / `Error`) via Android `SpeechRecognizer` (no new library).
  - `isLoading` flag while waiting for OpenAI.
  - `AutoFillProposal` — parsed from GPT-4o's structured JSON response (`action: fill_sets`) when in workout mode; user confirms before sets are written to Room.
  - `configure(sessionId, workoutMode)` — called by the host screen; controls which system prompt is built.
  - Two system prompt modes: in-workout (serializes current exercises + logged sets; asks for `fill_sets` JSON) and history Q&A (serializes `ExerciseProgressSummary` for all exercises; asks for `answer` JSON).
  - `applyAutoFill()` — matches fills to exercises by name, appends new `SetEntry` rows via `SetRepository`.
- **`AiAssistantSheet` composable** (`feature-workouts`) — `ModalBottomSheet` with:
  - Chat bubble list (user = primary colour, assistant = surfaceVariant, error = errorContainer).
  - `AutoFillBanner` — card with "Apply" + dismiss when proposal is ready.
  - Input row: mic toggle button, `OutlinedTextField`, send button.
- **AI button (SmartToy FAB)** added to both `ActiveWorkoutScreen` and `WorkoutsScreen` as a secondary FAB above the primary FAB.
- **`RECORD_AUDIO` permission** added to `AndroidManifest.xml`.
- **`kotlin.serialization` plugin + `kotlinx.serialization.json` dependency** added to `feature-workouts/build.gradle.kts`.

Validation: `./gradlew assembleDebug testDebugUnitTest --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Backlog cleanup — 2026-03-13

Autonomous session.

Delivered:
- **Exact date normalization** — movement history and workout-comparison chart labels now use the shared exact `YYYY-MM-DD` formatter instead of mixed short `M/d` labels.
- **Value-first emphasis pass** — movement list "Last logged" rows, movement session cards, and workout-comparison summaries now accent only the data values rather than styling the whole sentence uniformly.
- **Backlog cleanup** — removed stale idea notes for the already-shipped Movements tab split and shared movement detail routing so `ideas/` only reflects pending work.

Validation: `./gradlew :features:feature-history:clean --no-daemon`, `./gradlew testDebugUnitTest --no-daemon`, and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Design progress — workouts list status treatment

Autonomous session.

Delivered:
- Updated the workouts-list design backlog notes so the date/highlight pass now explicitly follows the broader design cleanup and uses the workouts list as the reference treatment.
- Added clearer workout-card state treatment on the Workouts screen: `Planned` vs `Logged` chips, stronger card border contrast for logged sessions, and a fallback `No movements yet` summary for empty cards.
- Moved the state chip to a right-aligned header position and replaced the low-value duplicate icon with the state icon itself so the right edge now communicates session status instead of a secondary action.
- Tightened the workout-card header again by collapsing the status treatment down to a single right-edge icon in the old action slot, keeping the green check / grey clock without increasing card height.
- Reduced workout-card summary density by splitting the counts into two compact icon-led stats for movements and sets instead of one longer sentence.
- Reduced bottom-right clutter on the Workouts screen by moving AI access into the top app bar and leaving the floating action button for the primary "new workout" action only.
- Moved the secondary session actions into a long-press card menu so tap remains the primary path into workout details while template-copy stays available without adding another visible icon.
- Added target-set progress to workout cards so sessions with programmed work now show a completion bar and `logged / target` set count at a glance.
- Clarified non-targeted card states so workouts without programmed set goals now distinguish `No movements yet`, `Ready to start logging`, and `Logged without set targets`.
- Tightened workout-list section headers so they now read as `Today`, `Yesterday`, weekday-only for recent sessions, and fuller weekday/date labels for older history instead of raw ISO dates.
- Split active target-tracked sessions out from completed ones so partially logged workouts now get a distinct in-progress icon and warmer card emphasis instead of looking fully done.

Validation: `./gradlew :features:feature-workouts:compileDebugKotlin --no-daemon` and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Date / highlight consistency progress

Autonomous session.

Delivered:
- Brought movement detail session history into line with the workouts list by grouping movement sessions under natural-language date headers instead of repeating a full date inside every card.
- Kept the per-session cards focused on the actual movement stats and navigation affordance, letting the section header carry the date context.

Validation: `./gradlew :features:feature-history:compileDebugKotlin --no-daemon` and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Movement terminology progress

Autonomous session.

Delivered:
- Tightened remaining user-facing workout/detail copy so visible actions and dialogs keep using `movement` language rather than mixing in `exercise`.
- Updated assistant/system prompt wording to prefer `movement` where that did not require changing compatibility-sensitive schema keys.
- Left internal type names, route params, and schema keys unchanged to avoid churn without direct UX benefit.

Validation: `./gradlew :features:feature-workouts:compileDebugKotlin --no-daemon`, `./gradlew :features:feature-history:compileDebugKotlin --no-daemon`, and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Backlog cleanup — stale idea pruning

Autonomous session.

Delivered:
- Removed the stale workouts-list reorder note because date section headers replaced the original “put the date first on each card” idea with a better grouping treatment.

Validation: backlog note cleanup only; no code changes.

---

## ✅ Workout chart design alignment

Autonomous session.

Delivered:
- Restyled the workout comparison cards to match the refined movement detail chart treatment more closely: stronger chart header, clearer gain summary, y-axis labels, and first/last session anchors instead of a dense row of date labels under every point.
- This replaces the older mini-chart presentation with the movement-detail visual baseline the backlog called for.

Validation: `./gradlew :features:feature-history:clean :features:feature-history:compileDebugKotlin --no-daemon`, `./gradlew :app:clean :app:compileDebugKotlin --no-daemon`, and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

---

## ✅ Splash polish progress

Autonomous session.

Delivered:
- Simplified the add-movement entry flow in the active workout screen: suggestion copy is now a larger `Suggestions` section, suggested names use stronger button treatment, and the manual path is labeled `Manually add` instead of relying on a lone plus icon.
- Added a proper platform splash theme path for `MainActivity`, using the existing launcher mark and launch palette so startup feels intentional without adding any artificial delay.
- Added the AndroidX splashscreen dependency and activity hook needed for the splash theme to work correctly on supported devices.

Validation: included in the app/workout chart validation above — BUILD SUCCESSFUL.

---

## ✅ Design2 progress

Autonomous session.

Delivered:
- Upgraded the movement detail PR section so the absolute top record stands out with stronger visual treatment and a trophy badge instead of all PR chips reading equally.
- Replaced the low-value “Open workout details” copy in movement session cards with a standard chevron disclosure treatment while keeping the cards tappable.
- Added an explicit `Start` CTA to planned and in-progress workout cards so zero-set sessions no longer read as passive placeholders.

Validation: `./gradlew :features:feature-history:compileDebugKotlin --no-daemon`, `./gradlew :features:feature-workouts:compileDebugKotlin --no-daemon`, and `./gradlew assembleDebug --no-daemon` — BUILD SUCCESSFUL.

Remaining:
- The leftover `design2` suggestions now require broader interaction/product decisions rather than small visual cleanup: skeleton launch UX, alternate landing destination, tappable chart points, and AI insight badges.

---

## Milestone 14 — Previous Performance in Exercise Card

Under the target line of each ExerciseCard in the active workout view, show a "Previous" line sourced from the most recent prior session for that exercise.

Desired format:
```
Previous: 5 reps × 92.5kg (6 days ago)
```
- Numbers (reps, weight, time) use the primary accent colour.
- "time ago" should be a rough human label: "today", "yesterday", "N days ago", "N weeks ago".
- If the previous session had multiple sets, show the best set (highest weight, or most reps if bodyweight).
- Only shown when a prior session exists for this exercise name.

Implementation notes:
- The last-performance data is already loaded in `ActiveWorkoutViewModel.lastPerformance` as `Map<String, List<SetEntry>>`.
- The session date for the previous workout is not currently available in `lastPerformance` — either extend the model or add a separate query `observeLastSessionDate(exerciseName)`.
- The "time ago" label can be computed from the previous session's date relative to now.

---

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
