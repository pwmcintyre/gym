# PRD: Build Performance

## Introduction

Build times directly affect iteration speed for every task in the repo. This PRD covers a deliberate audit and optimization pass — starting with measurement, then applying practical wins without adding fragile complexity.

## Goals

- Measure current build times to identify the slowest tasks
- Apply practical Gradle configuration improvements
- Improve day-to-day edit/build/test speed without making the project harder to work on

## User Stories

### US-001: Measure and document current build performance baseline
**Description:** As a developer, I want to know which Gradle tasks are slowest so I can prioritize optimization effort.

**Acceptance Criteria:**
- [ ] Run a clean build with `--profile` flag and capture the HTML report
- [ ] Identify the top 5 slowest tasks by duration
- [ ] Document findings in `tasks/build-performance-notes.md` with task names and durations
- [ ] Note which tasks are configuration-phase vs execution-phase
- [ ] Typecheck passes

### US-002: Enable Gradle configuration cache and build cache
**Description:** As a developer, I want Gradle to cache configuration and task outputs so repeated builds are faster.

**Acceptance Criteria:**
- [ ] `gradle.properties` updated with `org.gradle.configuration-cache=true` (or `warn` if strict mode causes failures)
- [ ] `gradle.properties` updated with `org.gradle.caching=true`
- [ ] Clean build followed by incremental build measured — second build is measurably faster
- [ ] No existing tasks broken by cache enablement
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)

### US-003: Tune Gradle JVM and parallel execution settings
**Description:** As a developer, I want Gradle to use available CPU cores and an appropriate heap so builds don't bottleneck on resources.

**Acceptance Criteria:**
- [ ] `org.gradle.parallel=true` confirmed set in `gradle.properties`
- [ ] `org.gradle.jvmargs` heap size reviewed — set to at least `-Xmx3g` if machine has sufficient RAM
- [ ] `org.gradle.daemon=true` confirmed enabled for local development
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)

### US-004: Audit and trim unnecessary Gradle task dependencies
**Description:** As a developer, I want the `compileDebugKotlin` (typecheck) task to run as fast as possible without pulling in unneeded work.

**Acceptance Criteria:**
- [ ] Run `./gradlew compileDebugKotlin --dry-run` and review the task graph
- [ ] Identify any tasks in the graph that are not necessary for compilation
- [ ] Remove or narrow any unnecessary dependencies found (e.g. resource processing tasks pulled in prematurely)
- [ ] `compileDebugKotlin` task graph is documented in `tasks/build-performance-notes.md`
- [ ] Typecheck still passes after any changes

## Functional Requirements

- FR-1: `gradle.properties` is the single place for build configuration tuning
- FR-2: All changes must be measurably beneficial — don't add config that has no effect
- FR-3: Document before/after timings for any meaningful change

## Non-Goals

- No module restructuring or code-level changes to improve build performance (out of scope for this PRD)
- No migration to new build tooling (e.g. Buck, Bazel)
- No CI/CD build optimization — local development only

## Technical Considerations

- Use `./gradlew assembleDebug --profile --no-daemon` to generate timing reports
- Configuration cache has known incompatibilities with some plugins — use `warn` mode if strict mode breaks the build
- Changes to `gradle.properties` take effect on the next Gradle invocation

## Success Metrics

- Incremental `compileDebugKotlin` build is faster than baseline (ideally under 15 seconds for a single-file change)
- Full `assembleDebug` time documented before and after

## Open Questions

- Are there any custom Gradle tasks or scripts that might be incompatible with configuration cache?
