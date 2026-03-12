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
2. Check `project.md` for active milestone context and any existing implementation notes.
3. Update `project.md` before or after substantial changes when milestone scope, status, or follow-up work becomes clearer.
4. Implement the smallest coherent increment.
5. Run relevant validation commands.
6. Fix failures before stopping.
7. Update documentation or milestone state.
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
- a short note in `project.md` or docs describing what changed,
- a git commit.

Suggested commit style:
- `feat(m1): bootstrap mobile app on device`
- `feat(m2): add workout entry and set logging`
- `feat(m3): add photo capture and OCR pipeline`

---

## Milestone tracking in project.md

Use `project.md` as the source of truth for:
- current milestone scope,
- acceptance criteria,
- milestone status,
- implementation notes,
- follow-up work and blockers.

Rules:
- Before starting meaningful work, inspect the active milestone in `project.md`.
- Keep milestone notes concise and high-signal.
- When discovering new work, add a short follow-up note to the relevant milestone section instead of keeping private notes.
- When blocked, record the blocker and the smallest alternative path in `project.md`.
- When completing work, immediately update milestone notes so the next session has current context.

Preferred note shape:
- current status,
- what changed,
- what remains,
- any blocker or dependency.

---

## Autonomy rules

The agent should work autonomously.
Do not wait for user confirmation for routine engineering choices.

You should:
- inspect the codebase,
- choose sensible defaults,
- run tests,
- fix straightforward failures,
- update `project.md`,
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
1. record the blocker in `project.md`,
2. capture attempted steps,
3. identify the smallest alternative path,
4. continue on unblocked work if possible.

---

## Session-start checklist

At the start of a session:
- read this file,
- inspect repository status,
- inspect `project.md` milestone status and notes,
- identify the active milestone,
- pick the next smallest valuable task,
- verify available run/test commands,
- begin implementation.

---

## Session-end checklist

Before stopping:
- run relevant validation,
- update `project.md`,
- update docs if needed,
- confirm repo state is clean or intentionally staged,
- commit if a milestone or meaningful verified increment is complete,
- leave a clear next task in `project.md`.

---

## Commands

### Setup
- Android SDK: `~/android-sdk` (set `ANDROID_HOME=~/android-sdk` or relies on `local.properties`)
- `local.properties` must contain: `sdk.dir=/home/pwm/android-sdk`
- Java: OpenJDK 17 (`java -version`)

### Validation
- full build: `./gradlew assembleDebug --no-daemon`
- unit tests: `./gradlew testDebugUnitTest --no-daemon`
- lint: `./gradlew lintDebug --no-daemon`
- type check (via build): `./gradlew compileDebugKotlin --no-daemon`
- single module build: `./gradlew :core:core-model:assembleDebug --no-daemon`

### Deploy
- Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
- Run on device: `adb shell am start -n com.gymapp/.MainActivity`

If commands change, update this file immediately.

---

## Definition of done

A task is done only when:
- the implementation is complete for the scoped task,
- relevant validation was run successfully,
- `project.md` context is updated,
- docs are updated if needed,
- the result is in a clean, reviewable state.

A milestone is done only when:
- the milestone acceptance criteria are satisfied,
- the app works end-to-end for that milestone,
- relevant tests/build checks pass,
- milestone notes are updated,
- a git commit has been created.
