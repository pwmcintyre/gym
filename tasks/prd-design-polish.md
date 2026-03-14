# PRD: Design Polish

## Introduction

Explore and implement the remaining design follow-ups for the workouts page. The core shipped features (tappable chart points, Today's focus card) are done. This PRD covers the one outstanding interaction experiment: lightweight AI insight states on the workouts page.

## Goals

- Determine whether a lightweight AI insight prompt adds value on the workouts page without adding clutter
- Ship it if it improves the experience; document and close it out if it doesn't add clear value
- Keep the workouts page fast, clean, and focused

## User Stories

### US-001: Evaluate and implement lightweight AI insight on workouts page
**Description:** As a user, I want to see a brief AI-generated insight on the workouts page so that I get useful context about my recent training without the screen feeling cluttered.

**Acceptance Criteria:**
- [ ] AI insight appears as a compact, non-intrusive element on the workouts page (e.g. below the Today's focus card or at the bottom of the screen)
- [ ] Insight is generated from recent workout data (last 7–14 days)
- [ ] Insight text is 1–2 sentences maximum — no walls of text
- [ ] Element is only shown when there is enough workout data to generate a meaningful insight (at least 3 sessions)
- [ ] If the AI call fails or returns nothing useful, the element is hidden gracefully (no error state visible to user)
- [ ] Loading state is handled with a subtle shimmer or placeholder — not a spinner that blocks the page
- [ ] Typecheck passes
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)

## Functional Requirements

- FR-1: Add an AI insight composable to the workouts screen, positioned below Today's focus card
- FR-2: Fetch insight asynchronously using the existing AI/OpenAI infrastructure in `core/core-ai/`
- FR-3: Insight prompt should summarize patterns from recent sessions (e.g. "You've done 4 upper-body sessions this week — consider adding a lower-body day")
- FR-4: Minimum data gate: skip the AI call entirely if fewer than 3 sessions exist in the last 14 days
- FR-5: Insight should refresh at most once per session launch, not on every recomposition

## Non-Goals

- No persistent storage of AI insights
- No user controls to dismiss or customize insights (keep it simple)
- No notification or background refresh
- Do not revive the PR-card direction (elevating one record above others)

## Technical Considerations

- Reuse `core/core-ai/` for the API call
- Keep the composable self-contained so it can be easily removed if the experiment fails
- Treat this as an experiment — if after implementation it feels cluttered or adds no value, remove it and close the story as "evaluated and rejected"

## Success Metrics

- Insight is visible and readable without scrolling on a standard phone screen
- No visible impact on page load time (insight loads asynchronously)
- Build and typecheck pass

## Open Questions

- Should the insight be personalized to today's planned workout or purely retrospective?
