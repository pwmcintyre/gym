# Normalize date and highlight treatment

Status: backlog idea

Reason:
- The app's visual language is inconsistent in a few places.
- Exact dates should read consistently, and emphasis should land on the data value rather than the whole sentence.

Scope:
- Standardize exact dates to `YYYY-MM-DD` across the app.
- Keep relative phrasing only where it is more useful than a literal date, such as "prior best" style copy that says `N days ago`.
- Audit highlighted text styles so only the number/value is emphasized, not the full surrounding sentence.

Implementation notes:
- Review workout lists, history/detail views, progress cards, and assistant/status copy for date formatting drift.
- Review chips, inline stats, helper text, and banners for inconsistent emphasis treatment.
- Prefer a shared formatting path or helper if multiple screens are duplicating this logic.
