---
name: ralph
description: "Convert PRDs to prd.json format for the Ralph autonomous agent system. Use when you have an existing PRD and need to convert it to Ralph's JSON format. Triggers on: convert this prd, turn this into ralph format, create prd.json from this, ralph json, run ralph on."
user-invocable: true
---

# Ralph PRD Converter

Converts existing PRDs to the `prd.json` format that Ralph uses for autonomous execution.

---

## The Job

Take a PRD (from `tasks/prd-*.md` or text) and write it to `prd.json` at the project root.

---

## Output Format

```json
{
  "project": "gym",
  "branchName": "ralph/[feature-name-kebab-case]",
  "description": "[Feature description from PRD title/intro]",
  "userStories": [
    {
      "id": "US-001",
      "title": "[Story title]",
      "description": "As a [user], I want [feature] so that [benefit]",
      "acceptanceCriteria": [
        "Criterion 1",
        "Criterion 2",
        "Typecheck passes"
      ],
      "priority": 1,
      "passes": false,
      "notes": ""
    }
  ]
}
```

---

## Story Size: The Number One Rule

**Each story must be completable in ONE Ralph iteration (one context window).**

Ralph spawns a fresh Claude instance per iteration with no memory of previous work. If a story is too big, it runs out of context before finishing.

### Right-sized stories:
- Add a Room entity field and update the DAO
- Add a Compose UI component to an existing screen
- Update a ViewModel with new state handling
- Add unit tests for a specific flow

### Too big (split these):
- "Build the workout history screen" → split into: Room query, ViewModel, UI components
- "Add AI scan feature" → split into: camera capture, API call, response parsing, UI
- "Refactor the data layer" → split into one story per repository or use case

**Rule of thumb:** If you cannot describe the change in 2–3 sentences, it is too big.

---

## Story Ordering: Dependencies First

Stories execute in priority order. Earlier stories must not depend on later ones.

**Correct order:**
1. Room schema changes (entities, DAOs)
2. Repository / use-case logic
3. ViewModel state
4. Compose UI that uses the ViewModel
5. Tests

---

## Acceptance Criteria: Must Be Verifiable

### Good criteria:
- "Add `modifier` column to `SetEntity` with default null"
- "Tapping a chart point shows session details sheet"
- "Typecheck passes (`./gradlew compileDebugKotlin --no-daemon`)"
- "Build passes (`./gradlew assembleDebug --no-daemon`)"
- "Tests pass (`./gradlew testDebugUnitTest --no-daemon`)"

### Bad criteria:
- "Works correctly"
- "Good UX"
- "Handles edge cases"

### Always include as final criteria:
```
"Typecheck passes (`./gradlew compileDebugKotlin --no-daemon`)"
"Build passes (`./gradlew assembleDebug --no-daemon`)"
```

For stories with testable logic, also include:
```
"Tests pass (`./gradlew testDebugUnitTest --no-daemon`)"
```

---

## Archiving Previous Runs

Before writing a new `prd.json`, check if one exists from a different feature:

1. Read current `prd.json` if it exists
2. If `branchName` differs from the new feature's branch, archive it:
   - Create `tasks/archive/YYYY-MM-DD-feature-name/`
   - Copy `prd.json` and `progress.txt` there
   - Reset `progress.txt` with a fresh header

`ralph.sh` handles this automatically when run, but do it manually when updating `prd.json` between runs.

---

## Checklist Before Saving

- [ ] Previous run archived (if `prd.json` exists with a different `branchName`)
- [ ] Each story is completable in one iteration
- [ ] Stories ordered by dependency (schema → repository → ViewModel → UI → tests)
- [ ] Every story has "Typecheck passes" and "Build passes" as criteria
- [ ] Stories with testable logic have "Tests pass" as criteria
- [ ] Acceptance criteria are verifiable (not vague)
- [ ] No story depends on a later story
