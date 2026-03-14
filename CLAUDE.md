# Ralph Agent Instructions

You are an autonomous coding agent working on the gym Android app.

## Your Task

1. Read the PRD at `prd.json` (project root)
2. Read the progress log at `progress.txt` (check Codebase Patterns section first)
3. Check you're on the correct branch from PRD `branchName`. If not, check it out or create from main.
4. Pick the **highest priority** user story where `passes: false`
5. Implement that single user story
6. Run quality checks (see Commands below)
7. Update `AGENTS.md` if you discover reusable patterns
8. If checks pass, commit ALL changes with message: `feat: [Story ID] - [Story Title]`
9. Update the PRD to set `passes: true` for the completed story
10. Append your progress to `progress.txt`

## Quality Checks

Run these in order — stop and fix before committing if any fail:

```sh
# Type check (fastest — run first)
./gradlew compileDebugKotlin --no-daemon

# Lint
./gradlew lintDebug --no-daemon

# Unit tests
./gradlew testDebugUnitTest --no-daemon

# Full build (run before marking a story complete)
./gradlew assembleDebug --no-daemon
```

For single-module changes, prefer the narrower form:
```sh
./gradlew :feature:feature-workouts:compileDebugKotlin --no-daemon
```

"Typecheck passes" in acceptance criteria = `compileDebugKotlin` succeeds.
"Tests pass" = `testDebugUnitTest` succeeds.

## Progress Report Format

APPEND to `progress.txt` (never replace):

```
## [Date/Time] - [Story ID]
- What was implemented
- Files changed
- **Learnings for future iterations:**
  - Patterns discovered
  - Gotchas encountered
  - Useful context
---
```

## Consolidate Patterns

Add reusable findings to the `## Codebase Patterns` section at the TOP of `progress.txt`:

```
## Codebase Patterns
- Room entities are in core/core-database/src/main/kotlin/com/gymapp/core/database/entity/
- Use fallbackToDestructiveMigration() — no schema migration files needed
- Hilt modules live in core/core-*/src/main/kotlin/.../di/
```

Only add patterns that are **general and reusable**, not story-specific.

## Update AGENTS.md

Before committing, check if any edited directories have learnings worth preserving. Add to `AGENTS.md` only **genuinely reusable knowledge** — API patterns, gotchas, non-obvious dependencies, testing approaches.

## Stop Condition

After completing a user story, check if ALL stories have `passes: true`.

If complete: `<promise>COMPLETE</promise>`

If stories remain: end your response normally (another iteration picks up the next story).

## Important

- Work on ONE story per iteration
- Commit only verified, building code — never commit broken code
- This is a PoC: use `fallbackToDestructiveMigration()`, no schema migrations
- Never use "volume" in user-facing strings
- Read `AGENTS.md` fully before starting — it contains critical project rules
