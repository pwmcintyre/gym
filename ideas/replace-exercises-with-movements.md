# Replace "exercise" terminology with "movement"

Status: backlog idea

Reason:
- "Exercise" is too gym-specific for the broader direction.
- Future tracked activities may include lifting, stretching, running, and similar movement types.

Scope:
- Replace user-facing "exercise"/"exercises" language across the app with "movement"/"movements" where that improves clarity.
- Audit for misspellings such as "excersizes" as part of the same pass.

Implementation notes:
- Prioritize user-facing copy first.
- Only rename internal types or database fields if the UX benefit justifies the churn.
- Keep third-party or protocol-specific terminology unchanged when required.

Progress notes:
- The main workout and history surfaces now consistently use `movement` in visible card actions, empty states, dialogs, and list summaries.
- Remaining `exercise` references are mostly internal model/API names and AI/system-prompt context rather than visible UI copy.
- The next useful slice would be auditing assistant-visible/generated text and any residual settings/help copy, then deciding whether internal renames are worth the churn.
