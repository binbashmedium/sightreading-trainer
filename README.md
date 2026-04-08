# Sightreading Trainer

An Android app that helps pianists and keyboard players build fluent music sight-reading skills through structured, interactive exercises using a USB or Bluetooth MIDI keyboard.

## Features

- Grand staff notation (treble + bass clef) with key signatures, accidentals, and ledger lines
- Real-time MIDI note matching with per-notehead color feedback (green = correct, red = wrong, yellow = extra)
- Exercise types: single notes, intervals, triads, arpeggios, clustered chords, sustain-pedal exercises
- Multi-key practice, configurable hand mode (left / right / both), adjustable exercise length and time limit
- Timed session mode with automatic exercise rollover
- Session statistics screen
- Auto-reconnect for USB and Bluetooth MIDI devices

## Architecture (Generator → Chord Detection → Display)

Current runtime flow:

1. **Exercise generation** (`GenerateExerciseUseCase`) creates an `Exercise` (list of `ExerciseStep`s with MIDI notes, rhythm values, pedal and ornaments).
2. **PracticeViewModel** starts the generated exercise through `PracticeSessionUseCase`.
3. **Android MIDI input** is captured by `AndroidMidiManager` as `NoteEvent` / pedal events.
4. **ChordDetector** groups near-simultaneous note-on events inside a time window into one `PerformanceInput`.
5. **PracticeSessionUseCase** compares the grouped player input to the expected current `ExerciseStep`.
6. **PracticeScreen / VerovioStaffView** render notation and feedback colors from mapped UI state.

In short: **generated target notes** + **detected played chord input** -> **matching** -> **visual feedback**.

### Extending architecture for non-generator note input (e.g., note database)

To support alternative sources (database/import) without coupling UI/domain to one source, introduce a small abstraction:

- `ExerciseSource` interface (example):  
  `fun load(settings: AppSettings, forcedKey: Int? = null): Exercise`
- Implementations:
  - `GeneratedExerciseSource` (current behavior using `GenerateExerciseUseCase`)
  - `DatabaseExerciseSource` (reads from `ExerciseLibraryRepository` and maps storage models to domain)
  - later: `FileImportExerciseSource` (MusicXML/MIDI from file)

Then keep the rest unchanged:

- `PracticeViewModel` requests an `Exercise` from `ExerciseSource`.
- `ChordDetector`, `PracticeSessionUseCase`, and rendering pipeline continue to operate on the same domain model (`ExerciseStep` with MIDI note lists).

This keeps one stable **canonical internal format** while allowing multiple upstream providers.

Implemented in code:

- `AppSettings.exerciseInputSource` selects `GENERATED` or `DATABASE`.
- `SettingsScreen` exposes source selection, persisted by `SettingsDataStore`.
- `ExerciseRepository` routes to `GeneratedExerciseSource` or `DatabaseExerciseSource` with generated fallback.
- `PracticeViewModel` remains unchanged and still consumes `ExerciseRepository.generateExercise(...)`.

### Recommended external score formats

For importing from a note database, common formats are:

- **MusicXML / compressed MXL** (best for notation semantics: measures, voices, articulations, ties, key/time signatures).
- **MIDI (.mid)** (best for timing/performance events; weaker notation semantics than MusicXML).
- **MEI** (rich scholarly notation format; useful if your tooling already uses MEI).

Practical recommendation for this app:

- Use **MusicXML** as primary exchange format for database storage/import.
- Convert imported data into internal `ExerciseStep` objects (`notes: List<Int>`, `noteValue`, pedal, ornaments).
- Optionally support **MIDI** as secondary import path by quantizing to note values and mapping simultaneous note-ons to chords.

## Requirements

- Android 10 (API 29) or later
- A USB or Bluetooth MIDI keyboard (optional for exploring the app)

## Local Android environment setup

If you want to reproduce instrumentation/screenshot checks locally, ensure your Android SDK is configured before running Gradle.

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"
```

Quick verification:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
```

## License

Copyright 2026 BinBashMedium

Licensed under the [Apache License, Version 2.0](LICENSE).

## F-Droid

This app is free and open-source software with no proprietary dependencies, no analytics, and no ads. It is designed to be publishable on [F-Droid](https://f-droid.org/).

App ID: `com.binbashmedium.sightreadingtrainer`

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Google Play Release Bundle (AAB)
# requires signing env vars (see below)
./gradlew bundleRelease

# Run all tests
./gradlew test
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Play Store bundle output: `app/build/outputs/bundle/release/app-release.aab`

### Release signing for Google Play

`bundleRelease` will create a signed `.aab` when the following environment variables are set:

- `KEYSTORE_PATH` (path to `.jks` or `.keystore`)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Example:

```bash
export KEYSTORE_PATH="$HOME/keys/upload-keystore.jks"
export KEYSTORE_PASSWORD="***"
export KEY_ALIAS="upload"
export KEY_PASSWORD="***"
./gradlew bundleRelease
```

## GitHub Actions Release Bundle

Für den manuellen Workflow `.github/workflows/android-release-bundle.yml` müssen diese GitHub Secrets gesetzt sein:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

### Upload-Keystore lokal erstellen (kurz)

Falls noch kein Upload-Key vorhanden ist, kannst du lokal z. B. so eine `.jks` erzeugen:

```bash
keytool -genkeypair \
  -v \
  -storetype JKS \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Keystore als Base64 codieren und als Secret speichern

```bash
base64 -w 0 upload-keystore.jks
```

Den ausgegebenen String vollständig als Secret `KEYSTORE_BASE64` in GitHub eintragen.

> Wichtig: Der Keystore darf **niemals** ins Repository committed werden.
