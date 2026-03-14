# gym

A local-first Android workout logging app for Pixel phones. Tracks exercises, sets, reps, and weight. Supports AI-assisted workout capture from gym whiteboard photos.

## Requirements

- macOS
- Java 17 (`brew install --cask temurin@17`)
- Android command-line tools (`brew install --cask android-commandlinetools`)

## First-time setup

```sh
# 1. Seed the Android SDK directory
mkdir -p ~/android-sdk/cmdline-tools
cp -r /opt/homebrew/share/android-commandlinetools/cmdline-tools/latest ~/android-sdk/cmdline-tools/latest

# 2. Accept SDK licenses
mkdir -p ~/android-sdk/licenses
printf '\n24333f8a63b6825ea9c5514f83c2829b004d1fee' > ~/android-sdk/licenses/android-sdk-license
printf '\n84831b9409646a918e30573bab4c9c91346d8abd' > ~/android-sdk/licenses/android-sdk-preview-license

# 3. Create local.properties (gitignored)
echo "sdk.dir=$HOME/android-sdk" > local.properties
```

The first build will auto-download `build-tools;34.0.0`, `platforms;android-35`, and `platform-tools`.

## Build

```sh
./gradlew assembleDebug --no-daemon
```

## Install and run on device

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.gymapp/.MainActivity
```

## Other commands

```sh
./gradlew testDebugUnitTest --no-daemon   # unit tests
./gradlew lintDebug --no-daemon           # lint
./gradlew compileDebugKotlin --no-daemon  # type check
```

## Stack

Kotlin · Jetpack Compose · Material3 · Room · Hilt · CameraX · DataStore · OpenAI Vision API

## Feature development with Ralph

New features and improvements are tracked as PRDs in `tasks/prd-*.md`. Autonomous implementation is handled by [Ralph](https://github.com/snarktank/ralph) — an agent loop that runs Claude Code repeatedly, completing one user story per iteration.

### Workflow

```sh
# 1. Create a PRD for the feature
#    Use the /prd skill or write tasks/prd-<feature>.md directly

# 2. Convert the PRD to prd.json
#    Use the /ralph skill — it writes prd.json at the project root

# 3. Run the agent loop
./ralph.sh --tool claude 10
```

Ralph will iterate until all stories have `passes: true`, then exit. Each iteration commits verified progress and appends learnings to `progress.txt`.

### Key files

| File | Purpose |
|------|---------|
| `tasks/prd-*.md` | Feature specs (backlog) |
| `prd.json` | Active PRD for the current Ralph run |
| `progress.txt` | Per-iteration learnings and codebase patterns |
| `CLAUDE.md` | Per-iteration instructions for Claude |
| `ralph.sh` | The agent loop |
| `tasks/archive/` | Completed runs |
