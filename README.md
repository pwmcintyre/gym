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
