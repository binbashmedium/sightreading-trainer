# Exercises

## Exercise Model (`domain/model/Exercise.kt`)

```kotlin
data class Exercise(
    val steps: List<ExerciseStep>,
    val currentIndex: Int = 0,
    val musicalKey: Int = 0,
    val handMode: HandMode = HandMode.RIGHT
)
```

`ExerciseStep` holds note groups plus optional per-note accidentals and optional pedal actions.
`currentStep` returns the step at `currentIndex`, or `null` if complete.
`isComplete` is true when `currentIndex >= steps.size`.

## GenerateExerciseUseCase (`domain/usecase/GenerateExerciseUseCase.kt`)

Creates an `Exercise` from current `AppSettings` using selected exercise content types, `handMode`, a selected-key pool, and `exerciseLength`.

The generator chooses one key from the selected key pool (or an externally forced key for in-session rollover), transposes the material by that key, then fills steps until the max displayed note budget (`exerciseLength`) is reached.

Exercises now use timed steps that can carry note groups, explicit note accidentals, and optional sustain-pedal actions per beat.
When note accidentals are disabled, generated notes are constrained to the current key's major scale, preventing out-of-scale labels (for example `Bb` in C major without an accidental).

### Exercise type pool

The generator builds each exercise from a selected pool of content types, for example:
- single notes
- octaves
- thirds
- fifths
- sixths
- triads
- sevenths
- ninths
- clustered chords

Chord-based material may also be emitted as arpeggio patterns instead of only stacked simultaneous notes.

If multiple types are selected, the generated exercise mixes them together. For example, selecting single notes and triads produces an exercise that contains both.

### Hand-mode behavior

- `HandMode.RIGHT` uses treble-range material.
- `HandMode.LEFT` uses left-hand material and renders on the bass staff.
- `HandMode.BOTH` alternates or combines left/right source material so the exercise spans both staves.

### Key constants

```kotlin
GenerateExerciseUseCase.KEY_NAMES  // ["C","C#","D","Eb","E","F","F#","G","Ab","A","Bb","B"]
```

Also exposed from `app/ui/GrandStaffModels.kt` as top-level `KEY_NAMES` for UI use.

## Practice UI Data Model (app layer)

Practice rendering is driven by the UI model in
`app/src/main/kotlin/com/binbashmedium/sightreadingtrainer/ui/GrandStaffModels.kt`:

```kotlin
enum class NoteState { NONE, CORRECT, WRONG, LATE }
enum class StaffType { TREBLE, BASS }

data class NoteEvent(
  val midi: Int,
  val startBeat: Float,
  val duration: Float,
  val expected: Boolean,
  val state: NoteState = NoteState.NONE,
  val staff: StaffType = StaffType.TREBLE
)

data class Chord(val name: String, val notes: List<Int>, val startBeat: Float)

data class GameState(
  val levelTitle: String,
  val elapsedTime: Long,
  val score: Int,
  val bpm: Float,
  val notes: List<NoteEvent>,
  val chords: List<Chord>,
  val currentBeat: Float,
  val musicalKey: Int = 0
)
```

## Staff Assignment

`staffForExercise(midi, handMode)` assigns staff during `toGameState(...)`:

- `HandMode.RIGHT` forces treble staff.
- `HandMode.LEFT` forces bass staff.
- `HandMode.BOTH` uses pitch split: `midi >= 60` -> treble, `midi < 60` -> bass.

## Staff Position Formula

`midiToDiatonicStep(midi)` converts a MIDI note to a diatonic step number (C-1=0, C4=28, E4=30).

`staffLineYForStep(step, staff, trebleTopY, bassTopY, lineSpacing)` is the shared vertical-position helper:
- Treble: `Y = (trebleTopY + 4*lineSpacing) - (step - 30) * lineSpacing/2`
- Bass:   `Y = (bassTopY  + 4*lineSpacing) - (step - 18) * lineSpacing/2`

The clefs are anchored to:
- Treble clef -> G4 line (`TREBLE_G_LINE_STEP = 32`)
- Bass clef -> F3 line (`BASS_F_LINE_STEP = 24`)

Ledger lines are drawn by `ledgerStepsBelow` / `ledgerStepsAbove`.

Current status: the treble clef now uses the same scale ratio as the bass clef, which brought it much closer to the proportion in `mocks/img.png` while keeping the corrected vertical placement. Latest fixes already in place:
- `CLEF_AREA_WIDTH_RATIO` raised to 3.8 to eliminate key-signature/clef overlap.
- `BASS_CLEF_BASELINE_FROM_TOP_RATIO` raised to 0.92 so the glyph renders below the A line.
- Spurious programmatic dot circles removed; the Unicode 𝄢 glyph already includes the two dots.
- `ACCIDENTAL_TEXT_SIZE_RATIO` increased to 2.2 and `ACCIDENTAL_SPACING_RATIO` to 0.75 for proportional key signatures.
- `BASS_CLEF_TEXT_SIZE_RATIO` reduced to 4.0 for correct glyph proportions.
- Cluster notehead displacement raised to `lineSpacing * 1.1` (one notehead width) per PDF page 6.
- Stem X for chord groups now anchors to the non-displaced noteheads (min x for upstem, max x for downstem).
- `Chord.staff` field added; labels render **above** the treble staff or **below** the bass staff.
- Label text size is dynamic (`beatWidth * 0.5f` clamped to `lineSpacing * 0.42f`) to prevent overlap.
- `formatChordLabelShort` used for labels (no Roman numeral) to save horizontal space.
Extracted PDF pages live under `validation/pdf-ref/`, device validation screenshots under `validation/screenshots20/`.

## Key Signature Rendering

`KEY_SIGNATURES[musicalKey]` returns `(sharps, flats)` for the generated key used by the current exercise.
Accidental glyphs are drawn at standard treble/bass staff positions using:
- `TREBLE_SHARP_STEPS`, `TREBLE_FLAT_STEPS`
- `BASS_SHARP_STEPS`, `BASS_FLAT_STEPS`

The standard accidental order and spacing are regression-tested against the notation reference.
Individual noteheads now render their own accidentals for black-key pitches, with staggered left columns for close-position chord spellings so adjacent accidentals do not collide.

Generated note accidentals are now optional in settings and use explicit `#`, `b`, and `natural` symbols rather than being inferred only from pitch class at render time.

## Notehead And Stem Rules

The app now applies these notation rules in pure helpers:
- notes on or above the middle line stem down
- notes below the middle line stem up
- chords use a single stem per staff/start-beat group
- stem direction for chords is controlled by the note farthest from the middle line; ties default to downstem
- seconds on one stem displace one notehead to avoid collisions
- ledger-line notes extend stems far enough to reach at least the middle line

## Mapping from Domain to UI State

`PracticeScreen.toGameState(nowMs)` maps `PracticeState` to `GameState`:
- `levelTitle` reflects the generated key chosen from the selected key pool
- `elapsedTime` = wall-clock elapsed since session start
- `score` and `bpm` come directly from `PracticeState`
- note colors and extra played-note overlays come from `PracticeState.inputByBeat[beatIndex]`:
  - matched expected notes -> green
  - missing expected notes -> red
  - extra played notes -> additional yellow noteheads
  - mixed-state chords render mixed notehead colors in the same beat (no blanket first-note color)
- `currentBeat` = `exercise.currentIndex * 2f` (static, input-driven cursor)

## Session Lifecycle

Any played step input (notes and optional pedal), correct or wrong, advances `exercise.currentIndex` by 1.
Pedal-only inputs (no notes) do not advance the exercise; they only update pedal state for the next played note/chord check.
When a chunk is complete before timeout, a new chunk is generated in the same key and the session continues.
Timeout finalization is now checked from both input handling and the UI timer tick (`PracticeViewModel.finalizeIfTimedOut(...)`), so session completion is persisted even when no new MIDI events arrive after time expires.
When session time is up, `PracticeScreen` shows `SessionCompleteOverlay` with final score, highscore, and correct/wrong note counts.
Expected pedal symbols are colored independently from note correctness (green/red), and unexpected pedal actions are rendered as additional yellow pedal marks.

Sustain-pedal press and release events are now represented directly on exercise steps so they can be checked alongside notes in the session pipeline.

## Chord Labels

Rendered chord labels should use harmonic names and roman numerals only when the notes form an actual chord, for example:
- `CM (I)`
- `Dm7 (ii7)`
- `GM9 (V9)`

Single notes should render as note names only and should not show harmonic chord labels.
Clustered chord voicings and inversions should still resolve to their harmonic chord labels rather than falling back to raw note names.

## Extending Exercises

1. Add generation logic in `GenerateExerciseUseCase`.
2. Ensure the output is a list of `ExerciseStep` entries (notes + optional accidentals + optional pedal action).
3. Update tests for the new hand-mode or exercise-length behavior if the pattern changes.

## Validation Artifacts

Recent device captures for notation and exercise-generation validation live under `validation/`, including:
- `validation/accidentals-arpeggios-check.png`
- `validation/accidentals-arpeggios-practice.png`
- `validation/validation-run1.png`
- `validation/validation-run2.png`
- `validation/validation-run3.png`
- `validation/validation-run4.png`
- `validation/validation-run5.png`
