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

## Requirements

- Android 10 (API 29) or later
- A USB or Bluetooth MIDI keyboard (optional for exploring the app)

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
