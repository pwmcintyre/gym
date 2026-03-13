# Bodyweight movement chart should show reps trend

Status: backlog idea

Reason:
- For bodyweight movements, weight is often fixed or not the most meaningful progress signal.
- Reps progression is usually the more useful charted metric in those cases.

Scope:
- When a movement is identified as bodyweight, show a `Reps trend` chart in place of the usual weight trend chart.
- Keep non-bodyweight movements on the existing weight-focused chart behavior.

Implementation notes:
- Reuse whatever bodyweight detection logic the app already has instead of introducing a separate chart-only rule.
- Confirm the chart title and axis labels stay clear and user-facing copy remains consistent.
