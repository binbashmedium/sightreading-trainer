# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Always first update the TODO.md with the tasks to do before doing the task. Keep the TODO.md always consistent with the current progress.
## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests (both modules)
./gradlew test

# Run only domain tests
./gradlew :domain:test

# Run only app unit tests
./gradlew :app:test

# Run a single test class
./gradlew :domain:test --tests "*.MatchNotesUseCaseTest"
./gradlew :app:test --tests "*.GrandStaffModelsTest"

# Run a single test method
./gradlew :domain:test --tests "*.MatchNotesUseCaseTest.someMethodName"
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Toolchain

| Component | Version |
|---|---|
| Kotlin | 2.1.21 |
| AGP | 8.8.0 |
| Gradle wrapper | 8.10.2 |
| KSP | 2.1.21-2.0.2 |
| Hilt | 2.57.1 |
| Compose BOM | 2025.05.01 |
| Java | 21 (set via `org.gradle.java.home` in gradle.properties) |

**Critical:** `kotlin.plugin.compose` plugin is mandatory for Kotlin 2.x (replaces the old `composeOptions {}`). Do not remove it. See `settings.gradle.kts` comments for why AGP appears in both `pluginManagement` and `buildscript` — this is intentional and required for KGP/AGP classloader compatibility.

## Architecture

Two Gradle modules:

- **`:domain`** — pure Kotlin/JVM, zero Android dependencies. All business logic lives here.
- **`:app`** — Android module, depends on `:domain`. All Android framework code, Compose UI, Hilt DI, and DataStore.

### Domain layer (`domain/`)

```
model/        NoteEvent, Exercise, MatchResult, PracticeState, AppSettings, NoteValue
usecase/      MatchNotesUseCase, GenerateExerciseUseCase, PracticeSessionUseCase
core/midi/    ChordDetector
core/util/    NoteNames
```

`Exercise` is an immutable snapshot of `List<List<Int>>` (chord sequences as MIDI note groups) plus a `currentIndex` cursor. `PracticeSessionUseCase` is the only stateful domain object — it wraps an `Exercise` and accumulates score across `processChord()` calls.

### App layer (`app/`)

```
core/midi/    AndroidMidiManager       — wraps android.media.midi, emits NoteEvent via SharedFlow
data/         SettingsDataStore, SettingsRepository, ExerciseRepository
di/           AppModule                — Hilt SingletonComponent bindings
ui/           GrandStaffModels.kt      — pure UI state types + render math (no Compose)
              PracticeScreen.kt        — GrandStaffCanvas + HeaderCard + toGameState mapper
              PracticeViewModel.kt, MainViewModel.kt, SettingsViewModel.kt
              SettingsScreen.kt, MainScreen.kt
```

### Data flow

```
USB MIDI → AndroidMidiManager (SharedFlow<NoteEvent>)
         → ChordDetector       (SharedFlow<List<NoteEvent>>)
         → PracticeViewModel   (calls PracticeSessionUseCase, holds StateFlow<PracticeState>)
         → PracticeScreen      (maps PracticeState → GameState via toGameState())
         → GrandStaffCanvas    (draws staff, notes, cursor via Canvas API)
```

### UI state mapping

`PracticeState` (domain) is never used directly in Compose. `PracticeScreen.toGameState(nowMs)` maps it to `GameState` (in `GrandStaffModels.kt`), which contains `List<NoteEvent>`, `List<Chord>`, beat position, and elapsed time. All staff rendering math (`midiToGrandStaffY`, `beatToX`, `durationToGlyphType`) lives in `GrandStaffModels.kt` — keep it Android-free so it stays unit-testable without Robolectric.

### Staff rendering conventions

- Grand staff split: `midi >= 60` → treble; `midi < 60` → bass.
- Beat → pixel: `startX + (beat * beatWidth)` where `beatWidth = max(30f, canvasWidth / 24f)`.
- Each exercise chord occupies 2 beats (`startBeat = chordIndex * 2f`).
- `NoteState` colors: NONE=black, CORRECT=green `#2E7D32`, WRONG=red `#C62828`, LATE=yellow `#F9A825`.

### Settings & DI

`AppSettings` (DataStore-backed) flows from `SettingsRepository` into all ViewModels. `PracticeViewModel` reads it to configure `AndroidMidiManager`, `ChordDetector` (chordWindowMs), and `MatchNotesUseCase` (toleranceMs). `GenerateExerciseUseCase` uses `selectedExerciseTypes` (multi-select `ExerciseContentType`), `selectedKeys`, and `handMode` (LEFT/RIGHT/BOTH).

## Known Gaps (see TODO.md)

No open gaps — all previously listed items have been resolved.


## Keep all Context files upda to date

Context files up to date. They are located in /context

## At the end all test needs to pass. Additional tests must be added when new logic is added.
## Also a apk must be compiled