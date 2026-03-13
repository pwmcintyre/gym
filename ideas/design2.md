# Remaining design2 follow-ups

Status: backlog idea

Reason:
- Most of the original `design2` recommendations are already shipped.
- The remaining value is in the few deeper interaction ideas that still have not been tried.

Scope:
- Tappable chart-point previews are now shipped on the movement progress chart: tapping a point selects that session and exposes an `Open workout` action.
- Explore lightweight AI insight states on the workouts page only if they add clear value without cluttering the screen.
- A lightweight `Today's focus` state is now shipped on the workouts page when a session exists for today.

Implementation notes:
- Do not revive the older PR-card direction that elevated one record above the other; the current preferred direction is two equal headline metrics.
- Do not treat splash plumbing as pending here; platform splash support is already in place.
- Keep this note focused on interaction/product experiments, not already-shipped visual cleanup.
