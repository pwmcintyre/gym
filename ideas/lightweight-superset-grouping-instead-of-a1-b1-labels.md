# Lightweight superset grouping instead of A1/B1 labels

Status: backlog idea

Reason:
- Labels like `A1` and `B1` reflect whiteboard notation, but feel arbitrary once imported into the app.
- In practice, the leading letter usually represents a superset group and the number identifies the movement within that group.

Scope:
- Introduce a lightweight superset concept so related movements can be grouped together in the UI.
- Reframe labels such as `A1` and `A2` as members of superset `A`, rather than treating the full token as the main standalone identifier.
- Prefer a presentation-oriented grouping model first, avoiding unnecessary complexity in data entry or storage unless it proves needed.

Implementation notes:
- Start by grouping movements visually under a superset heading or container while preserving current workout logging simplicity.
- Keep solo movements straightforward; they should not require explicit superset setup.
- Review scan/import handling so whiteboard labels can still be parsed, but presented in a more meaningful way inside the app.
