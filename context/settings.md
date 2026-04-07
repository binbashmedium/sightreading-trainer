# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val exerciseTimeMin: Int = 1,
    val exerciseMode: ExerciseMode = ExerciseMode.CLASSIC,
    val exerciseTypes: Set<ExerciseContentType> = setOf(ExerciseContentType.SINGLE_NOTES),
    val handMode: HandMode = HandMode.RIGHT,
    val noteAccidentalsEnabled: Boolean = false,
    val pedalEventsEnabled: Boolean = false,
    val highScore: Int = 0,
    val totalCorrectNotes: Int = 0,
    val totalWrongNotes: Int = 0,
    val correctGroupStats: Map<String, Int> = emptyMap(),
    val wrongGroupStats: Map<String, Int> = emptyMap(),
    val correctNoteStats: Map<String, Int> = emptyMap(),
    val wrongNoteStats: Map<String, Int> = emptyMap(),
    val soundEnabled: Boolean = true,
    /** Selectable pool of keys (0 = C … 11 = B). */
    val selectedKeys: Set<Int> = setOf(0),
    /** Active chord progressions used when PROGRESSIONS type is selected. */
    val selectedProgressions: Set<ChordProgression> = setOf(ChordProgression.I_IV_V_I),
    /** Mode-2 voicing options used for progression generation. */
    val progressionExerciseTypes: Set<ProgressionExerciseType> = setOf(ProgressionExerciseType.TRIADS),
    val selectedNoteValues: Set<NoteValue> = NoteValue.entries.toSet(),
    val chordNamesEnabled: Boolean = false,
    val bassNoteRangeMin: Int = 28,
    val bassNoteRangeMax: Int = 60,
    val trebleNoteRangeMin: Int = 60,
    val trebleNoteRangeMax: Int = 84,
    /** Which ornament types (TRILL, MORDENT, TURN) may appear. Empty = no ornaments. */
    val selectedOrnaments: Set<OrnamentType> = emptySet()
)

enum class HandMode { LEFT, RIGHT, BOTH }
enum class ExerciseMode { CLASSIC, PROGRESSIONS }
enum class ExerciseContentType {
    SINGLE_NOTES,
    OCTAVES,
    THIRDS,
    FIFTHS,
    SIXTHS,
    ARPEGGIOS,
    TRIADS,
    SEVENTHS,
    NINTHS,
    CLUSTERED_CHORDS
}
enum class ProgressionExerciseType { TRIADS, SEVENTHS, NINTHS, ARPEGGIOS }
```

## Persistence

Settings are persisted using Jetpack DataStore (`Preferences`).

- `SettingsDataStore` maps preference keys to fields, including the selected key pool.
- Highscore and cumulative note counters are persisted in DataStore as part of `AppSettings`.
- Group/note performance statistics are persisted in DataStore as serialized maps:
  - `correctGroupStats`, `wrongGroupStats`
  - `correctNoteStats`, `wrongNoteStats`
- `SettingsRepository` exposes `Flow<AppSettings>` and the save API.
- `SettingsViewModel` and other consumers collect from the repository flow.

## Runtime Usage

### Practice pipeline
`PracticeViewModel` reads settings to configure:
- `AndroidMidiManager.openDevice(settings.midiDeviceName)`
- `ChordDetector(chordWindowMs = settings.chordWindowMs)`
- `PracticeSessionUseCase.processInput(..., timingToleranceMs = settings.timingToleranceMs)`

### Exercise generation
`GenerateExerciseUseCase` uses:
- a multi-select pool of exercise content types (`exerciseTypes`)
- `exerciseTimeMin`
- `handMode`
- `noteAccidentalsEnabled`
- `pedalEventsEnabled`
- `selectedKeys` (0-11 pool; one key is chosen per exercise)
- `selectedProgressions` (used when `exerciseMode = PROGRESSIONS`)
- `progressionExerciseTypes` (`TRIADS`/`SEVENTHS`/`NINTHS`/`ARPEGGIOS` mode-2 options)
- `selectedNoteValues` (which note durations may appear in measures)
- `bassNoteRangeMin`/`bassNoteRangeMax` (MIDI 28–72 clamp for bass-staff notes)
- `trebleNoteRangeMin`/`trebleNoteRangeMax` (MIDI 48–93 clamp for treble-staff notes)
- `selectedOrnaments` (TRILL/MORDENT/TURN; empty set = no ornaments)

## SettingsScreen UI

The screen includes:
- exercise-mode chips (`Mode 1` classic types, `Mode 2` progressions)
- exercise-type multi-select chips for Mode 1 (`single notes` → `clustered chords`)
- progression-voicing chips for Mode 2 (`triads`, `sevenths`, `ninths`, `arpeggios`)
- exercise-time slider
- chord-progression multi-select chips (shown only in Mode 2)
- multi-select key chips
- hand-mode chips
- generated note-accidental toggle
- generated pedal-mark toggle
- ornament type multi-select chips (Trill, Mordent, Turn; none required)
- note-value multi-select chips (Whole, Half, Quarter, Eighth; at least one required)
- note-range sliders (bass min/max, treble min/max)
- timing tolerance slider
- chord-window slider
- sound toggle
- MIDI device radio list

## Statistics Screen

Main navigation now includes a `Statistics` page with Top 5 sections:
- correct groups
- incorrect groups
- correct notes
- incorrect notes

Group stats are aligned with exercise types (for example `SINGLE_NOTES`, `TRIADS`, `SEVENTHS`) via `ExerciseStep.contentType`.

## UI Notes

The chosen generated key and `handMode` are embedded into `Exercise` at generation time so the practice screen can keep rendering the correct staff assignment, key signature, and title even if settings change mid-session.

Single-note generation is allowed to include larger melodic skips such as fifth-based motion; the exercise-type chips are not restricted to scalar-only movement. Clustered chord voicings and arpeggios are also selectable as their own exercise content types.

Exercise length is fixed at `GenerateExerciseUseCase.DEFAULT_EXERCISE_MEASURES = 16` measures (one portrait page: 4 rows × 4 measures) and is no longer user-configurable. Each measure is filled with one of four uniform note-value patterns (WHOLE, 2×HALF, 4×QUARTER, 8×EIGHTH) chosen randomly per measure.
