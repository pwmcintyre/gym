# PRD: Assistant UI Overlay

## Introduction

The workouts page currently exposes two different assistant entry points with mismatched visual treatments. One of them uses a "coach" concept that is directionally good, but the current full-page takeover is too heavy for the page. This PRD captures a redesign to unify the assistant entry point and present the conversation in a compact overlay pattern that feels like chatting above the workout screen rather than navigating away from it.

## Goals

- Remove the duplicate assistant buttons on the workouts page
- Consolidate the assistant entry point into one clear, consistent UI
- Keep workout context visible while chatting with the assistant
- Make the assistant feel lightweight, fast, and obviously attached to the current workout screen

## Status

Implemented on `ralph/ai-assistant`.

Shipped in this branch:
- The old per-screen `SmartToy` assistant buttons were removed from workouts and active-workout screens
- The global green coach FAB is now the only assistant trigger
- The coach opens as an animated floating panel above the FAB instead of a full-page takeover
- Cold-launch opening now waits until the initial coach reply is ready before revealing the panel

Follow-up note:
- Keyboard behavior was re-validated on a connected Pixel-sized device after switching IME handling to lift the overlay instead of shrinking it.

## User Stories

### US-001: Replace duplicate assistant entry points with one consistent trigger
**Description:** As a user, I want a single obvious way to open the assistant from the workouts page so that the UI feels coherent and predictable.

**Acceptance Criteria:**
- [x] The workouts page shows only one assistant trigger
- [x] The trigger uses a single consistent visual style aligned with the preferred "coach" direction
- [x] No secondary legacy assistant button remains on the screen
- [x] Build passes (`./gradlew assembleDebug --no-daemon`)

### US-002: Present assistant chat as an overlay instead of a full-page takeover
**Description:** As a user, I want the assistant to open in a partial-height sheet or floating chat surface so that I can keep workout context visible while chatting.

**Acceptance Criteria:**
- [x] Opening the assistant does not replace the full workouts page
- [x] The assistant appears as either:
- [x] a half-height bottom sheet, or
- [x] a floating chat bubble that expands into a compact overlay panel
- [x] The underlying workouts UI remains visible behind or around the assistant
- [x] The overlay can be dismissed quickly with a clear close/minimize action
- [x] The overlay layout works on a standard Pixel phone without covering critical workout controls by default
- [x] Build passes (`./gradlew assembleDebug --no-daemon`)

### US-003: Preserve conversational feel without overwhelming the screen
**Description:** As a user, I want the assistant to feel like an in-context chat layer so that it is useful during workouts without dominating the page.

**Acceptance Criteria:**
- [x] Assistant messages are visually presented as chat content inside the overlay
- [x] The initial open state is compact rather than fully expanded to the entire screen
- [x] Motion between closed and open states feels intentional and lightweight
- [x] The design clearly communicates that I am chatting "above" the workouts page
- [x] The UI remains touch-friendly during a workout session

### US-004: Keep the coach panel usable when the keyboard opens
**Description:** As a user, I want the coach panel to stay readable while typing so that I can see both the conversation and my current input when the keyboard is open.

**Acceptance Criteria:**
- [x] Opening the keyboard does not squash the coach panel into an unreadable height
- [x] The chat surface shifts upward when the keyboard appears instead of compressing all content vertically
- [x] The keyboard may cover the lower portion of the screen, but the conversation remains readable above it
- [x] The text input remains visible while typing
- [x] The most recent assistant and user messages remain visible or easily recoverable when the IME opens
- [x] Behavior is verified on a standard Pixel-sized device

## Functional Requirements

- FR-1: Remove or replace the current duplicate assistant buttons on the workouts page
- FR-2: Use one assistant entry point only
- FR-3: Render assistant interaction in an overlay container anchored to the workouts page
- FR-4: Keep workout content visible while the assistant is open
- FR-5: Support quick dismiss/minimize behavior suitable for in-workout use
- FR-6: Handle IME insets by translating or resizing the assistant overlay in a way that preserves message readability and input visibility

## Non-Goals

- No expansion of assistant capabilities or prompt logic in this task
- No new assistant backend behavior
- No redesign of unrelated workouts page cards or navigation patterns

## Technical Considerations

- Prefer the smallest Compose implementation that can be tested quickly on-device
- Reuse the stronger existing "coach" visual direction where possible, but adapt it to an overlay presentation
- Validate the final interaction on an actual phone because screen occupancy is the main issue

## Success Metrics

- The workouts page has one assistant trigger instead of two
- Assistant interaction no longer feels like a page takeover
- Users can reference workout content while the assistant is open
- Typing in the assistant remains usable with the keyboard open

## Open Questions

- Resolved: use the floating bubble/panel approach rather than a bottom sheet
- Resolved: the global coach persists across navigation because it lives at the app root
