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
    AppSettings.kt        - all user-configurable settings + exerciseTimeSec + stats/highscore + selected key/type pools + selected scale mode
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
    StatisticsScreen.kt + StatisticsViewModel.kt
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
   The session timer start is primed from the first successful staff render only once per session, so row/page changes do not reset elapsed time.
4. `GrandStaffCanvas` draws a connected grand staff with staff lines behind the clefs, a dedicated clef area, the generated key signature, harmonic chord labels, pedal marks, note glyphs, stem-direction-aware chord groups, and a static cursor at the current expected step.
   Note/pedal coloring is now per-symbol from `PracticeState.inputByBeat` snapshots (matched expected = green, missing expected = red, extra played = yellow).
   Chord groups now preserve per-notehead color (mixed-state chords are not flattened to one color).
5. The single "New Exercise" button regenerates notes via `PracticeViewModel.reloadSession()`.

## Orientation-Aware Layout

`PracticeScreen` detects orientation via `LocalConfiguration.current`:

- **Portrait**: 4 grand-staff rows stacked vertically in a `Column`. Each row is a `GrandStaffCanvas` with a `[startBeat, endBeat)` window of 32 beat-units (4 measures × `BEATS_PER_MEASURE_UNITS`). The red cursor is only visible in the row that contains `currentBeat`. When the cursor reaches the last row of a page, the page flips to show the next 4 rows.
- **Landscape**: Single `GrandStaffCanvas` showing a 4-measure window (32 beat-units). The visible window advances when the cursor leaves the current page (`landscapePage = (currentBeat / BEATS_PER_ROW).toInt()`).

### Beat / Layout Constants (`GrandStaffModels.kt`)

```kotlin
const val BEATS_PER_STEP          = 2f    // UI beat-units per quarter note
const val MEASURES_PER_ROW        = 4     // portrait: 4 measures per row
const val ROWS_PER_PAGE           = 4     // portrait: 4 rows per page
const val BEATS_PER_MEASURE_UNITS = 8f    // 4 quarter-note beats × BEATS_PER_STEP
const val BEATS_PER_ROW           = 32f   // MEASURES_PER_ROW × BEATS_PER_MEASURE_UNITS
const val BEATS_PER_PAGE          = 128f  // ROWS_PER_PAGE × BEATS_PER_ROW

// Extension: converts NoteValue to UI beat-units
val NoteValue.uiBeatUnits: Float get() = beats * BEATS_PER_STEP
// WHOLE=8f, HALF=4f, QUARTER=2f, EIGHTH=1f
```

`NoteValue.beats` (domain): WHOLE=4f, HALF=2f, QUARTER=1f, EIGHTH=0.5f.
`uiBeatUnits` maps musical beats to the UI coordinate system.

### Note Values and Measure Patterns

Each measure in 4/4 time must contain exactly one of these uniform patterns:
- `[WHOLE]` — 1 whole note per measure
- `[HALF, HALF]` — 2 half notes per measure
- `[QUARTER, QUARTER, QUARTER, QUARTER]` — 4 quarter notes per measure
- `[EIGHTH ×8]` — 8 eighth notes per measure

`GenerateExerciseUseCase.MEASURE_PATTERNS` holds all 4 patterns. `applyMeasurePatterns()` randomly selects one pattern per measure and assigns `noteValue` to each `ExerciseStep`.

### Helper Functions

```kotlin
fun beatToPage(beat: Float): Int          // which page number a beat falls on
fun pageStartBeat(page: Int): Float       // first beat-unit of a given page
fun rowMeasureLabel(rowStartBeat: Float): Int  // 1-based measure number for a row
```

### Bar Lines

`GrandStaffCanvas` draws vertical bar lines at every `beatsPerMeasure` interval within the visible beat range. The final bar line is drawn thicker (3f stroke) at `endBeat`; intermediate bar lines use 1.5f stroke.

## Repository Hygiene

Der frühere Ordner `validation/` mit manuellen Prüf-Artefakten wurde aus dem Repository entfernt.
Aktuelle technische Dokumentation liegt in `context/` und laufende Aufgaben in `TODO.md`.

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
- `statistics` -> `StatisticsScreen`
