# Architecture

## Overview

The app follows **Clean Architecture** with two Gradle modules:

```
:domain   — pure Kotlin, no Android dependencies
:app      — Android module, depends on :domain
```

A new UI-oriented score renderer was added in the app layer. Domain state remains unchanged and is mapped to a dedicated UI state model before rendering.

## Module Structure

### `:domain`
Contains all business logic. Has zero Android dependencies.

```
domain/
  model/
    NoteEvent.kt          — raw MIDI note-on event (midiNote, velocity, timestamp)
    Exercise.kt           — sequence of expected note groups, current position + musicalKey
    MatchResult.kt        — sealed class: Correct | Incorrect | TooEarly | TooLate | Waiting
    PracticeState.kt      — session snapshot: exercise, score, resultByBeat, bpm, lastCorrectTimestamp
    AppSettings.kt        — all user-configurable settings + HandMode enum + musicalKey (0–11)
    NoteValue.kt          — rhythmic duration enum (WHOLE/HALF/QUARTER/EIGHTH) with beats: Float helper
  usecase/
    MatchNotesUseCase.kt       — compares played chord against expected notes and checks timing
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
    AndroidMidiManager.kt — wraps android.media.midi, emits domain NoteEvent via SharedFlow
  data/
    SettingsDataStore.kt  — reads/writes AppSettings to DataStore<Preferences>
    SettingsRepository.kt — exposes settings as Flow<AppSettings>
    ExerciseRepository.kt — delegates to GenerateExerciseUseCase
  di/
    AppModule.kt          — Hilt @Module providing use-case singletons
  ui/
    GrandStaffModels.kt   — UI state + render helpers (GameState/Chord/UI NoteEvent/NoteState)
    MainScreen.kt + MainViewModel.kt
    PracticeScreen.kt + PracticeViewModel.kt
    SettingsScreen.kt + SettingsViewModel.kt
```

## Data Flow

```
USB MIDI keyboard
      │  android.media.midi
      ▼
AndroidMidiManager           (SharedFlow<domain.NoteEvent>)
      │
      ▼
ChordDetector                groups events within chordWindowMs
      │  SharedFlow<List<domain.NoteEvent>>
      ▼
PracticeViewModel            collects chords, calls PracticeSessionUseCase
      │  StateFlow<PracticeState>
      ▼
PracticeScreen               maps PracticeState -> ui.GameState
      │
      ▼
GrandStaffCanvas             draws one Grand Staff + dynamic chords/notes/cursor via Canvas
```

## UI Rendering Pipeline (Practice)

1. `PracticeScreen` starts a session on `LaunchedEffect(Unit)` by calling `PracticeViewModel.startSession()`.
2. Domain `PracticeState` is transformed into UI `GameState` (`toGameState` mapper).
3. `HeaderCard` shows: level title (key · type), elapsed time, live BPM, and score.
4. `GrandStaffCanvas` draws exactly one connected grand staff (treble + bass), chord labels, note glyphs, and a **static** cursor at the current expected chord.
5. Single "New Exercise" button regenerates notes via `PracticeViewModel.reloadSession()`.

## Dependency Injection

Hilt is used with `SingletonComponent` for app-scoped dependencies.
`AndroidMidiManager` is `@Singleton` injected into `PracticeViewModel`.
Use-cases are provided via `AppModule` as singletons.
Settings flow from `SettingsRepository` (DataStore-backed) into ViewModels.

## Navigation

Single-activity Compose navigation with three destinations:
- `main` → `MainScreen`
- `practice` → `PracticeScreen`
- `settings` → `SettingsScreen`
