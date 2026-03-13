# Normalize date and highlight treatment

Status: backlog idea

Reason:
- The app's visual language is inconsistent in a few places.
- Exact dates should read consistently, and emphasis should land on the data value rather than the whole sentence.
- This pass should follow the broader workouts-list design cleanup so the final date treatment matches the updated visual hierarchy instead of being applied twice.

Scope:
- Standardize exact dates to `YYYY-MM-DD` across the app.
- Keep relative phrasing only where it is more useful than a literal date, such as "prior best" style copy that says `N days ago`.
- Audit highlighted text styles so only the number/value is emphasized, not the full surrounding sentence.
- After the workouts-list design pass, apply the same grouping language to movement history/detail flows so movement sessions read like the workouts list rather than isolated dated cards.

Progress notes:
- The movement detail history list now uses workouts-list style date section headers (`Today`, `Yesterday`, weekday names, fuller weekday/date labels) instead of repeating a literal date inside every session card.
- The next useful slice is deciding where exact literal `YYYY-MM-DD` still needs to stay visible versus where grouped natural-language headers are the better treatment.

Implementation notes:
- Review workout lists, history/detail views, progress cards, and assistant/status copy for date formatting drift.
- Treat the workouts list grouping as the reference treatment, then bring movement detail/session-history lists into line with it.
- Review chips, inline stats, helper text, and banners for inconsistent emphasis treatment.
- Prefer a shared formatting path or helper if multiple screens are duplicating this logic.
