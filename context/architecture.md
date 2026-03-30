# Architecture

## Overview

The app follows **Clean Architecture** with two Gradle modules:

```
:domain   - pure Kotlin, no Android dependencies
:app      - Android module, depends on :domain
```

The practice UI maps domain state into a dedicated grand-staff render model before drawing.

## Module Structure

### `:domain`
Contains all business logic. Has zero Android dependencies.

```
domain/
  model/
    NoteEvent.kt          - raw MIDI note-on event (midiNote, velocity, timestamp)
    Exercise.kt           - sequence of ExerciseStep entries, current position + generated musicalKey + handMode
    ExerciseStep.kt       - per-beat notes + per-note accidentals + optional pedal action
    MatchResult.kt        - sealed class: Correct | Incorrect | TooEarly | TooLate | Waiting
    PracticeState.kt      - session snapshot: exercise, timer, score, correct/wrong note counts, resultByBeat, bpm
    AppSettings.kt        - all user-configurable settings + exerciseTimeSec + stats/highscore + selected key/type pools
    PerformanceInput.kt   - grouped notes + pedal action passed to matching
    NoteValue.kt          - rhythmic duration enum (WHOLE/HALF/QUARTER/EIGHTH) with beats: Float helper
  usecase/
    MatchNotesUseCase.kt       - compares played input (notes + pedal) against expected ExerciseStep
    GenerateExerciseUseCase.kt - creates Exercise from selected exercise types + hand mode + max-note budget
    PracticeSessionUseCase.kt  - stateful session: advances through steps, accumulates score + note counters
core/
  midi/
    ChordDetector.kt      - groups note-on + pedal changes within a time window into one PerformanceInput
  util/
    NoteNames.kt          - MIDI number to note name (for example 60 -> C4)
```

### `:app`
Android-specific layer. Depends on `:domain`.

```
app/
  SightReadingApp.kt      - @HiltAndroidApp entry point
  MainActivity.kt         - single Activity, hosts Compose NavHost
  core/midi/
    AndroidMidiManager.kt - wraps android.media.midi, emits domain NoteEvent via SharedFlow
  data/
    SettingsDataStore.kt  - reads/writes AppSettings to DataStore<Preferences>
    SettingsRepository.kt - exposes settings as Flow<AppSettings>
    ExerciseRepository.kt - delegates to GenerateExerciseUseCase
  di/
    AppModule.kt          - Hilt @Module providing use-case singletons
  ui/
    GrandStaffModels.kt   - UI state + render helpers (GameState/Chord/UI NoteEvent/NoteState)
    MainScreen.kt + MainViewModel.kt
    PracticeScreen.kt + PracticeViewModel.kt
    SettingsScreen.kt + SettingsViewModel.kt
```

## Data Flow

```
USB MIDI keyboard
      |
      v
AndroidMidiManager           (SharedFlow<domain.NoteEvent>)
      |
      v
ChordDetector                groups notes + pedal events within chordWindowMs
      |
      v
PracticeViewModel            collects grouped input, resets detector state on new sessions, calls PracticeSessionUseCase, rolls exercises in same key until timer ends, and finalizes timeout from both MIDI and UI timer paths
      |
      v
PracticeScreen               maps PracticeState -> ui.GameState
      |
      v
GrandStaffCanvas             draws the grand staff, clefs, notes, cursor, and labels
```

## UI Rendering Pipeline (Practice)

1. `PracticeScreen` starts a session on `LaunchedEffect(Unit)` by calling `PracticeViewModel.startSession()`.
2. Domain `PracticeState` is transformed into UI `GameState` with `toGameState(nowMs)`.
3. `HeaderCard` shows level title, elapsed time, live BPM, and score.
4. `GrandStaffCanvas` draws a connected grand staff with staff lines behind the clefs, a dedicated clef area, the generated key signature, harmonic chord labels, pedal marks, note glyphs, stem-direction-aware chord groups, and a static cursor at the current expected step.
   Note/pedal coloring is now per-symbol from `PracticeState.inputByBeat` snapshots (matched expected = green, missing expected = red, extra played = yellow).
   Chord groups now preserve per-notehead color (mixed-state chords are not flattened to one color).
5. The single "New Exercise" button regenerates notes via `PracticeViewModel.reloadSession()`.

## Dependency Injection

Hilt is used with `SingletonComponent` for app-scoped dependencies.
`AndroidMidiManager` is `@Singleton` injected into `PracticeViewModel`.
Use-cases are provided via `AppModule` as singletons.
Settings flow from `SettingsRepository` (DataStore-backed) into ViewModels.

## Navigation

Single-activity Compose navigation with three destinations:
- `main` -> `MainScreen`
- `practice` -> `PracticeScreen`
- `settings` -> `SettingsScreen`
