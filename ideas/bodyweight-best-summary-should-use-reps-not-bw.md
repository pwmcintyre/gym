# Bodyweight best summary should use reps instead of BW

Status: backlog idea

Reason:
- `Best BW` reads like a weight metric even though bodyweight progress is usually measured by reps performed.
- The summary should match the actual signal users care about for bodyweight movements.

Scope:
- For bodyweight movements, replace the `Best BW` summary label/value treatment with a reps-based best metric such as `Best 12`.
- Keep the existing best-weight summary behavior for non-bodyweight movements.

Implementation notes:
- Align this with the bodyweight chart logic so chart and summary cards present the same progress story.
- Ensure the final user-facing copy avoids awkward phrasing while staying compact in the UI.
