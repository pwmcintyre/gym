# Gym Workout Logging App — Product & Engineering Plan

## Overview

A **local-first Android app** for logging gym workouts and automatically generating workout templates by scanning a gym whiteboard.

The app is optimized for **personal use on a Pixel phone** and prioritizes:

* Fast logging during workouts
* Offline operation
* Durable local storage
* Optional cloud backup
* AI-assisted workout extraction from photos

The app **avoids full backend hosting**. All workout data is stored locally, with optional backup to Google Drive.

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

| Component       | Technology                       |
| --------------- | -------------------------------- |
| Language        | Kotlin                           |
| UI              | Jetpack Compose                  |
| Architecture    | MVVM + Unidirectional Data Flow  |
| Local Storage   | Room (SQLite)                    |
| Camera          | CameraX                          |
| Background Jobs | WorkManager                      |
| Backup          | Google Drive API (appDataFolder) |

---

## AI Recognition

Whiteboard parsing will use **OpenAI Vision models**.

Because API keys must not be stored in mobile apps, the system will use a **minimal proxy service**.

### Proxy Responsibilities

* Receive image from app
* Send request to OpenAI
* Return structured JSON response
* Enforce request size limits

This proxy does **not store any data**.

Recommended hosting:

* Cloud Run
* Firebase Functions
* Any small serverless endpoint

---

# Architecture Overview

```
Android App
    │
    ├── Room Database (source of truth)
    │
    ├── Google Drive Backup
    │
    ├── CameraX
    │
    └── AI Scan Service
            │
            └── Proxy Server
                    │
                    └── OpenAI Vision Model
```

---

# Core Data Model

## ExerciseTemplate

Reusable exercise definition.

```kotlin
ExerciseTemplate
- id
- name
- defaultNotes
```

---

## WorkoutSession

A recorded workout.

```kotlin
WorkoutSession
- id
- date
- notes
- createdAt
```

---

## ExerciseEntry

Exercise within a workout session.

```kotlin
ExerciseEntry
- id
- workoutSessionId
- label (A1, B1, etc)
- exerciseName
- targetPrescription
- notes
```

---

## TargetPrescription

Defines planned workout targets.

```kotlin
TargetPrescription
- sets: Int?
- reps: Int?
- modifier: RepModifier
- rawText: String?
```

---

## RepModifier

```kotlin
enum class RepModifier {
    NONE,
    MAX
}
```

Rules:

| Source Text | Result                   |
| ----------- | ------------------------ |
| `3-4 reps`  | reps = 4                 |
| `MAX`       | modifier = MAX           |
| `4 x MAX`   | sets = 4, modifier = MAX |

---

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

---

# Storage Strategy

## Source of Truth

All workout data stored locally in **Room database**.

Benefits:

* instant access
* offline support
* durable storage

---

## Backup

Periodic backup to **Google Drive appDataFolder**.

Characteristics:

* private to the app
* hidden from user
* auto-restorable

Backup format:

```
JSON snapshot
```

Example:

```json
{
  "workouts": [],
  "exercises": [],
  "sets": []
}
```

---

# AI Whiteboard Recognition

## Process

1. User captures photo
2. Image sent to proxy
3. Proxy sends image to OpenAI
4. Model extracts workout structure
5. App receives JSON
6. App normalizes output
7. User reviews and edits

---

# AI Output JSON Schema

Expected response:

```json
{
  "workout_date": null,
  "items": [
    {
      "label": "A1",
      "exercise_name": "Back Squat",
      "target_sets": 4,
      "target_reps": 8,
      "rep_modifier": "NONE",
      "notes": "3s pause at bottom",
      "raw_source_text": "A1 Back Squat 4 x 8 3s pause at bottom"
    }
  ]
}
```

---

# Normalization Rules

After AI output:

| Input        | Result         |
| ------------ | -------------- |
| `3-4 reps`   | reps = 4       |
| `max`        | modifier = MAX |
| missing sets | sets = null    |
| missing reps | reps = null    |

All original text preserved in `raw_source_text`.

---

# App Modules

```
app/

core/
    model/
    database/
    drivebackup/
    ai/
    camera/

features/
    workouts/
    history/
    scan/
    settings/
```

---

# Milestones

---

# Milestone 1 — App Foundation

Goal: Working Android app with persistent storage.

### Features

* App launches on device
* Basic navigation
* Room database initialized
* Workout data models defined
* Settings screen
* Camera permissions
* Google Drive auth scaffold

### Acceptance Criteria

* App installs on Pixel
* Data persists across restarts
* Test workout can be saved

---

# Milestone 2 — Manual Workout Logging

Goal: Fully usable manual logging experience.

### Features

Exercise library.

Workout creation.

Per-exercise targets.

Per-set logging.

Workout history.

Edit past workouts.

### Logging UI

Each exercise shows:

```
Exercise Name
Target: 4 x 8

Set 1  Weight  Reps
Set 2  Weight  Reps
Set 3  Weight  Reps
Set 4  Weight  Reps
```

### Acceptance Criteria

* Multiple exercises per workout
* Multiple sets per exercise
* Editing previous sessions
* Offline operation

---

# Milestone 3 — AI Whiteboard Scan

Goal: Generate workouts from whiteboard photos.

### Flow

```
Scan Workout
     ↓
Take Photo
     ↓
AI Parse
     ↓
Review & Edit
     ↓
Start Workout
```

### Features

* Camera capture
* Photo import
* AI parsing
* Editable preview
* Convert to workout template

### Acceptance Criteria

* Workout extracted from photo
* User can correct mistakes
* Result saved as workout session

---

# Non-Functional Requirements

## Performance

| Requirement     | Target     |
| --------------- | ---------- |
| App startup     | <2 seconds |
| Save workout    | instant    |
| Scan processing | <5 seconds |

---

## Reliability

* Local-first
* Offline logging
* Background backup retries
* Atomic database writes

---

## Privacy

* No user accounts required
* Photos not stored after processing
* AI processing optional in future

---

# UX Requirements

### Gym-Optimized Logging

* Large tap targets
* Minimal typing
* Fast weight entry
* Clear set progression

---

### Scan Editing

User correction must be easier than retyping.

Example:

```
A1 Back Squat
Sets: 4
Reps: 8
Notes: 3s pause

[edit]
```

---

# Future Enhancements

Possible future milestones.

## Milestone 4

Progress tracking.

Charts.

PR tracking.

---

## Milestone 5

Workout suggestions.

Auto progression.

---

## Milestone 6

Cross-device sync.

Shared workouts.

---

# Key Design Decisions

| Decision       | Reason                   |
| -------------- | ------------------------ |
| Native Android | Best performance         |
| Compose UI     | Modern Android UI        |
| Room DB        | Reliable offline storage |
| Drive backup   | No backend required      |
| OpenAI Vision  | Robust parsing           |
| JSON export    | Easy debugging           |

---

# Final Summary

This system creates a **fast, local-first gym app** that:

* Runs entirely on-device
* Stores workouts locally
* Backs up to Google Drive
* Uses AI to parse whiteboard workouts
* Requires almost no backend infrastructure

The architecture is **simple, durable, and scalable** while remaining easy for a coding agent to implement.

