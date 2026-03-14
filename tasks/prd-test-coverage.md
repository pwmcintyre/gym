# PRD: Test Coverage

## Introduction

Test coverage is uneven across the app. The highest-risk flows — workout creation/editing, movement history/progress, and parsing — have little or no automated coverage. This PRD adds focused, maintainable tests to the areas most likely to regress.

## Goals

- Add lightweight unit tests for the highest-risk product flows
- Prefer narrow tests that protect real behavior over broad, brittle coverage targets
- Use the smallest effective test type: unit tests first, integration/UI only when unit coverage isn't enough

## User Stories

### US-001: Add unit tests for workout creation and editing
**Description:** As a developer, I want unit tests for the workout creation and editing flows so regressions are caught before they reach the device.

**Acceptance Criteria:**
- [ ] Tests cover: creating a new workout session, adding a set to an existing session, editing set values (weight, reps, modifier)
- [ ] Tests exercise the ViewModel or use-case layer, not just data models
- [ ] Tests use fakes or in-memory Room database — no mocks of the database itself
- [ ] All new tests pass (`./gradlew testDebugUnitTest --no-daemon`)
- [ ] Typecheck passes

### US-002: Add unit tests for movement history and progress behavior
**Description:** As a developer, I want unit tests for movement history aggregation so changes to how progress is calculated are caught early.

**Acceptance Criteria:**
- [ ] Tests cover: retrieving movement history for a given exercise, computing best set per session, ordering sessions by date
- [ ] Tests use an in-memory Room database populated with known data
- [ ] Edge cases covered: no sessions, single session, sessions with multiple sets
- [ ] All new tests pass
- [ ] Typecheck passes

### US-003: Add unit tests for workout import / parsing flows
**Description:** As a developer, I want unit tests for the AI scan parsing logic so changes to prompt output handling don't silently break import.

**Acceptance Criteria:**
- [ ] Tests cover: parsing a well-formed AI response into workout data, handling a malformed/partial response gracefully, handling an empty response
- [ ] Tests are pure unit tests (no network calls — use hardcoded JSON/text fixtures)
- [ ] All new tests pass
- [ ] Typecheck passes

### US-004: Add integration tests for fragile UI state logic
**Description:** As a developer, I want integration-level tests for the most fragile UI state flows so state bugs are caught before manual testing.

**Acceptance Criteria:**
- [ ] Tests cover at minimum: the workouts list screen loading state, the set-editor showing correct initial values when editing an existing set
- [ ] Tests use Compose testing APIs (no Espresso unless necessary)
- [ ] Tests run in the `testDebugUnitTest` or `connectedDebugAndroidTest` suite as appropriate
- [ ] All new tests pass
- [ ] Typecheck passes

## Functional Requirements

- FR-1: New tests live in the appropriate `src/test/` or `src/androidTest/` directories of the relevant feature or core module
- FR-2: Use in-memory Room (`Room.inMemoryDatabaseBuilder`) for any tests that need database access — never mock the Room DAO directly
- FR-3: Use `kotlinx-coroutines-test` for ViewModel/coroutine testing
- FR-4: Keep test fixtures small and explicit — avoid large JSON blobs

## Non-Goals

- No 100% line coverage target
- No screenshot testing
- No performance/load testing
- No test infrastructure changes (CI, coverage reporting) — just the tests themselves

## Technical Considerations

- Check existing test dependencies in `gradle/libs.versions.toml` before adding new ones
- Compose testing libraries may need to be added to the feature module's `build.gradle.kts`
- In-memory Room tests require the `room-testing` artifact

## Success Metrics

- All new tests pass in CI (`./gradlew testDebugUnitTest --no-daemon`)
- Workout creation, movement history, and parsing flows all have at least one test each
- No new test flakiness introduced

## Open Questions

- Are there existing test utilities (fakes, builders) in the repo that should be reused?
