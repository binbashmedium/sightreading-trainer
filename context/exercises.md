# Exercises

## Domain Model

`Exercise` is an immutable sequence of `ExerciseStep` values plus `currentIndex`, `musicalKey`, and `handMode`.

`ExerciseStep` may contain:
- `notes: List<Int>`
- `noteValue: NoteValue` (`WHOLE`/`HALF`/`QUARTER`/`EIGHTH`)
- optional accidentals per note
- optional pedal action
- optional ornament
- optional progression label notes / source content type metadata

## Generation (`GenerateExerciseUseCase`)

The generator builds one exercise page of fixed length:
- `DEFAULT_EXERCISE_MEASURES = 16`
- `MATERIAL_POOL_SIZE = 128`

Each measure gets one uniform rhythmic pattern from:
- `1 × WHOLE`
- `2 × HALF`
- `4 × QUARTER`
- `8 × EIGHTH`

Patterns are filtered by `selectedNoteValues` first (strict preference), with safe fallback if selection is invalid/empty.

## Exercise Modes

### Mode 1 — `CLASSIC`
Mixes selected `exerciseTypes` (single notes, intervals, triads, sevenths, ninths, clustered chords, arpeggios).

### Mode 2 — `PROGRESSIONS`
Uses `selectedProgressions` + `progressionExerciseTypes` (triads/sevenths/ninths/arpeggios).
Progression order is advanced **chord-by-chord** and preserved through the generated sequence; rhythm patterns only change spacing/duration.

## Scale/Key/Range Constraints

Generation uses:
- one selected key from `selectedKeys`
- active `selectedScaleType` (Major, Harmonic Minor, Melodic Minor, Pentatonic, Blues)
- bass/treble range clamps from settings

Then post-processes accidentals, pedal marks, and ornaments according to settings.

## Source Abstraction

`ExerciseRepository` loads from `ExerciseSource`:
- `GeneratedExerciseSource`
- `DatabaseExerciseSource`

If database source yields no steps, repository falls back to generated output.

## UI Mapping

`PracticeScreen.toGameState(nowMs)` maps `PracticeState` into `GameState`.
Beat positions are cumulative (sum of previous `NoteValue.uiBeatUnits`), including cursor position.

Chord labels are derived from harmonic detection helpers in `GrandStaffModels.kt`, including inversion slash notation and progression-aware label notes.
