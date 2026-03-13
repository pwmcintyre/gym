# Split progress overview into a dedicated movements page

Status: backlog idea

Reason:
- The Workouts page is doing two jobs at once: session browsing and movement discovery.
- The current `Progress Overview` card hides movement history behind a secondary card instead of giving it a first-class place in navigation.
- A dedicated movements page would make the existing drill-in to movement details easier to find while keeping the workouts list focused on sessions.

Scope:
- Remove the `Progress Overview` card from the Workouts page.
- Add a top-level `Movements` page that lists all known movements.
- Let users tap a movement to open the existing movement detail / progress screen.
- Keep the Workouts page centered on workout sessions, templates, and starting a new workout.

Implementation notes:
- Reuse the existing progress summary data source and detail screen rather than building a separate analytics flow.
- Default the user-facing label to `Movements` to match ADR-0001, unless later UX testing shows `Exercises` is clearer.
- Start with a simple list ordered by recent activity; add search or filters only if the basic split feels insufficient.
