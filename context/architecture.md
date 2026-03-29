# Architecture

## Overview

The app follows **Clean Architecture** with two Gradle modules:

```
:domain   — pure Kotlin, no Android dependencies
:app      — Android module, depends on :domain
```

## Module Structure

### `:domain`
Contains all business logic. Has zero Android dependencies.

```
domain/
  model/
    NoteEvent.kt          — raw MIDI note-on event (midiNote, velocity, timestamp)
    Exercise.kt           — sequence of chords to play, tracks current position
    MatchResult.kt        — sealed class: Correct | Incorrect | TooEarly | TooLate | Waiting
    PracticeState.kt      — snapshot of an active session (exercise, score, lastResult)
    AppSettings.kt        — all user-configurable settings + HandMode enum
  usecase/
    MatchNotesUseCase.kt      — compares played chord against expected, checks timing
    GenerateExerciseUseCase.kt — creates Exercise for a given difficulty + hand mode
    PracticeSessionUseCase.kt  — stateful session: advances through exercise, accumulates score
core/
  midi/
    ChordDetector.kt      — groups note-on events within a time window into chords
  util/
    NoteNames.kt          — MIDI number → note name (e.g. 60 → "C4")
```

### `:app`
Android-specific layer. Depends on `:domain`.

```
app/
  SightReadingApp.kt      — @HiltAndroidApp entry point
  MainActivity.kt         — single Activity, hosts Compose NavHost
  core/midi/
    AndroidMidiManager.kt — wraps android.media.midi, emits NoteEvent via SharedFlow
  data/
    SettingsDataStore.kt  — reads/writes AppSettings to DataStore<Preferences>
    SettingsRepository.kt — exposes settings as Flow<AppSettings>
    ExerciseRepository.kt — delegates to GenerateExerciseUseCase
  di/
    AppModule.kt          — Hilt @Module providing all use-case singletons
  ui/
    MainScreen.kt + MainViewModel.kt
    PracticeScreen.kt + PracticeViewModel.kt
    SettingsScreen.kt + SettingsViewModel.kt
```

## Data Flow

```
USB MIDI keyboard
      │  android.media.midi
      ▼
AndroidMidiManager          (SharedFlow<NoteEvent>)
      │
      ▼
ChordDetector               groups events within chordWindowMs
      │  SharedFlow<List<NoteEvent>>
      ▼
PracticeViewModel           collects chords, calls PracticeSessionUseCase
      │  StateFlow<PracticeState>
      ▼
PracticeScreen              renders staff, current chord, feedback colour
```

## Dependency Injection

Hilt is used with `SingletonComponent` for all app-scoped dependencies.
`AndroidMidiManager` is `@Singleton` injected into `PracticeViewModel`.
Use-cases are provided via `AppModule` as singletons.
Settings flow from `SettingsRepository` (DataStore-backed) into ViewModels.

## Navigation

Single-activity Compose navigation with three destinations:
- `main` → `MainScreen`
- `practice` → `PracticeScreen`
- `settings` → `SettingsScreen`
