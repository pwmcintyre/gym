# AGENTS.md

## Project purpose

This repository contains a mobile gym app intended to run on a Pixel phone.
The project is being built iteratively in milestones.

Primary product goals:
1. Ship a working app that runs on the phone.
2. Add workout tracking with exercises, sets, reps, weight, and optional set modifiers.
3. Add photo-based workout/program capture using OpenAI-powered recognition.

The agent should optimize for steady, testable progress rather than broad speculative refactors.

---

## Working style

- Work milestone by milestone.
- Prefer the smallest viable implementation that satisfies the current milestone.
- Do not jump ahead to future milestones unless it is required to avoid rework.
- Keep changes tightly scoped.
- Make the app usable early, then improve.

When in doubt:
- choose simplicity over cleverness,
- choose boring reliability over novelty,
- choose shippable progress over architecture astronautics.

The user is interested in modern frameworks and strong UX, but not at the expense of delivery.
You may adopt newer tools only if they clearly improve developer velocity or user experience without making autonomous progress harder.

---

## Required execution loop

For every task, follow this loop:

1. Understand the current milestone and acceptance criteria.
2. Check Beads for active and blocked work.
3. Create or update Beads tasks before making substantial changes.
4. Implement the smallest coherent increment.
5. Run relevant validation commands.
6. Fix failures before stopping.
7. Update documentation or task state.
8. Commit only when the increment is working and verified.

Do not leave the repository in a partially broken state.

---

## Milestone delivery rules

- Treat each milestone as a separately shippable checkpoint.
- Complete milestones in order unless a prerequisite forces a small exception.
- After finishing a milestone, make a git commit before starting the next one.
- Commit between milestones is mandatory.
- Prefer additional small commits within a milestone if they help preserve clean history, but do not spam meaningless commits.

Each milestone should end with:
- working code,
- passing relevant tests,
- a short note in docs or Beads describing what changed,
- a git commit.

Suggested commit style:
- `feat(m1): bootstrap mobile app on device`
- `feat(m2): add workout entry and set logging`
- `feat(m3): add photo capture and OCR pipeline`

---

## Task tracking with Beads

Use `bd` for task tracking.

Beads is the source of truth for:
- current tasks,
- dependencies,
- blocked items,
- milestone progress,
- implementation notes.

Rules:
- Before starting meaningful work, inspect current Beads state.
- Represent each milestone as an epic or parent task.
- Represent implementation steps as child tasks or dependent tasks.
- Keep tasks small enough to complete in one focused session.
- Mark blocked tasks explicitly.
- When discovering new work, add it to Beads rather than keeping private notes.
- When completing work, immediately update Beads status.
- Regularly check in on task graph health: ready tasks, blocked tasks, stale tasks, and scope creep.

Minimum Beads cadence:
- At session start: review ready tasks.
- Before coding: confirm the active task.
- After coding: update status and record any follow-up work.
- Before ending: ensure the next ready task is visible.

If Beads is not initialized:
1. initialize it,
2. create milestone tasks,
3. create the next actionable child task,
4. proceed.

Example task shape:
- Milestone 1: app boots on Pixel device
  - set up project scaffold
  - confirm Android build runs
  - add basic navigation shell
  - document local run/test workflow

---

## Autonomy rules

The agent should work autonomously.
Do not wait for user confirmation for routine engineering choices.

You should:
- inspect the codebase,
- choose sensible defaults,
- run tests,
- fix straightforward failures,
- update tasks,
- commit verified progress.

Escalate only for decisions that materially affect:
- product scope,
- paid services,
- secrets or credentials,
- destructive migrations,
- irreversible vendor lock-in,
- legal/privacy-sensitive behavior.

If there is uncertainty but a safe default exists, choose the safe default and continue.

---

## Testing and self-verification

The agent must be able to test its own progress.

Every code change should be validated by one or more of:
- unit tests,
- integration tests,
- linting,
- type checking,
- build verification,
- emulator/device run verification,
- screenshot or UI smoke checks where available.

Always run the narrowest relevant checks first, then broader checks before declaring completion.

Minimum rule:
- Never claim a task is done unless you ran at least one relevant verification command.
- Never commit code that obviously fails lint, typecheck, tests, or build if those checks exist.

Preferred validation order:
1. targeted tests for changed code,
2. lint/typecheck for affected area,
3. full project build,
4. app launch verification for milestone-level changes.

If tests do not exist for an area that is likely to regress:
- add lightweight tests where practical,
- otherwise add a short note explaining the verification that was performed.

---

## Mobile-app expectations

This is a phone-first app.
Optimize for:
- simple flows,
- fast startup,
- reliable local state,
- touch-friendly UI,
- good offline behavior where practical,
- readable forms during workouts.

For the current product direction:
- storage should default to the chosen local-first approach already decided by the user,
- workout entry should support integer sets/reps/weight plus an optional modifier,
- current modifier support should be minimal and explicit, starting with `max`.

Do not overbuild analytics, syncing, auth, or cloud infrastructure before needed by a milestone.

---

## Preferred implementation behavior

- Prefer clear folder structure and predictable naming.
- Keep dependencies minimal.
- Avoid introducing heavy frameworks unless justified.
- Avoid speculative abstractions.
- Avoid rewriting working code just to match a personal preference.
- Preserve momentum.

When adding a dependency, document why it is needed.

When choosing between:
- a trendy option that may slow autonomous progress, and
- a standard option the agent can implement and test reliably,

prefer the standard option.

---

## Documentation updates

Update docs when behavior changes materially.

At minimum, keep current:
- setup instructions,
- run instructions,
- test instructions,
- milestone status,
- any non-obvious architectural decision.

Do not write long design docs unless the change is substantial.
Prefer concise, high-signal updates.

---

## Git rules

- Work on the current branch unless instructed otherwise.
- Keep commits focused and reviewable.
- Commit only verified work.
- Do not rewrite history unless explicitly asked.
- Do not commit secrets, generated credentials, or large unnecessary artifacts.
- Before committing, review diff for accidental noise.

Required commit checkpoint:
- one commit at the completion of each milestone.

---

## Safety and boundaries

Do not:
- fabricate test results,
- mark tasks complete without verification,
- skip task tracking,
- silently ignore failures,
- introduce secrets into source control,
- make destructive schema or file operations without clear need,
- expand scope into future milestones unnecessarily.

If blocked:
1. record the blocker in Beads,
2. capture attempted steps,
3. identify the smallest alternative path,
4. continue on unblocked work if possible.

---

## Session-start checklist

At the start of a session:
- read this file,
- inspect repository status,
- inspect Beads ready/blocked tasks,
- identify the active milestone,
- pick the next smallest valuable task,
- verify available run/test commands,
- begin implementation.

---

## Session-end checklist

Before stopping:
- run relevant validation,
- update Beads,
- update docs if needed,
- confirm repo state is clean or intentionally staged,
- commit if a milestone or meaningful verified increment is complete,
- leave a clear next task in Beads.

---

## Commands

### Setup
- Android SDK: `~/android-sdk` (set `ANDROID_HOME=~/android-sdk` or relies on `local.properties`)
- `local.properties` must contain: `sdk.dir=/home/pwm/android-sdk`
- Java: OpenJDK 17 (`java -version`)
- Beads: `export PATH="$HOME/.local/bin:$PATH" && bd dolt start` (then `bd status`)

### Validation
- full build: `./gradlew assembleDebug --no-daemon`
- unit tests: `./gradlew testDebugUnitTest --no-daemon`
- lint: `./gradlew lintDebug --no-daemon`
- type check (via build): `./gradlew compileDebugKotlin --no-daemon`
- single module build: `./gradlew :core:core-model:assembleDebug --no-daemon`

### Deploy
- Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
- Run on device: `adb shell am start -n com.gymapp/.MainActivity`

### Task tracking
- beads start: `export PATH="$HOME/.local/bin:$PATH" && bd dolt start`
- beads status: `bd status`
- list ready tasks: `bd ready`
- create task: `bd create --type task --title "..." --body "..." --parent <epic-id>`
- update task: `bd update <id> --status in_progress|closed`
- show task: `bd show <id>`

If commands change, update this file immediately.

---

## Definition of done

A task is done only when:
- the implementation is complete for the scoped task,
- relevant validation was run successfully,
- Beads status is updated,
- docs are updated if needed,
- the result is in a clean, reviewable state.

A milestone is done only when:
- the milestone acceptance criteria are satisfied,
- the app works end-to-end for that milestone,
- relevant tests/build checks pass,
- milestone notes are updated,
- a git commit has been created.

<!-- BEGIN BEADS INTEGRATION -->
## Issue Tracking with bd (beads)

**IMPORTANT**: This project uses **bd (beads)** for ALL issue tracking. Do NOT use markdown TODOs, task lists, or other tracking methods.

### Why bd?

- Dependency-aware: Track blockers and relationships between issues
- Git-friendly: Dolt-powered version control with native sync
- Agent-optimized: JSON output, ready work detection, discovered-from links
- Prevents duplicate tracking systems and confusion

### Quick Start

**Check for ready work:**

```bash
bd ready --json
```

**Create new issues:**

```bash
bd create "Issue title" --description="Detailed context" -t bug|feature|task -p 0-4 --json
bd create "Issue title" --description="What this issue is about" -p 1 --deps discovered-from:bd-123 --json
```

**Claim and update:**

```bash
bd update <id> --claim --json
bd update bd-42 --priority 1 --json
```

**Complete work:**

```bash
bd close bd-42 --reason "Completed" --json
```

### Issue Types

- `bug` - Something broken
- `feature` - New functionality
- `task` - Work item (tests, docs, refactoring)
- `epic` - Large feature with subtasks
- `chore` - Maintenance (dependencies, tooling)

### Priorities

- `0` - Critical (security, data loss, broken builds)
- `1` - High (major features, important bugs)
- `2` - Medium (default, nice-to-have)
- `3` - Low (polish, optimization)
- `4` - Backlog (future ideas)

### Workflow for AI Agents

1. **Check ready work**: `bd ready` shows unblocked issues
2. **Claim your task atomically**: `bd update <id> --claim`
3. **Work on it**: Implement, test, document
4. **Discover new work?** Create linked issue:
   - `bd create "Found bug" --description="Details about what was found" -p 1 --deps discovered-from:<parent-id>`
5. **Complete**: `bd close <id> --reason "Done"`

### Auto-Sync

bd automatically syncs via Dolt:

- Each write auto-commits to Dolt history
- Use `bd dolt push`/`bd dolt pull` for remote sync
- No manual export/import needed!

### Important Rules

- ✅ Use bd for ALL task tracking
- ✅ Always use `--json` flag for programmatic use
- ✅ Link discovered work with `discovered-from` dependencies
- ✅ Check `bd ready` before asking "what should I work on?"
- ❌ Do NOT create markdown TODO lists
- ❌ Do NOT use external issue trackers
- ❌ Do NOT duplicate tracking systems

For more details, see README.md and docs/QUICKSTART.md.

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

<!-- END BEADS INTEGRATION -->
