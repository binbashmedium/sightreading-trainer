# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val exerciseTimeSec: Int = 60,
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
    val selectedKeys: Set<Int> = setOf(0)
)

enum class HandMode { LEFT, RIGHT, BOTH }
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
- a multi-select pool of exercise content types
- `exerciseTimeSec`
- `handMode`
- `noteAccidentalsEnabled`
- `pedalEventsEnabled`
- `selectedKeys` (0-11 pool; one key is chosen per exercise)

## SettingsScreen UI

The screen includes:
- exercise-type multi-select chips
- exercise-time slider
- multi-select key chips
- hand-mode chips
- generated note-accidental toggle
- generated pedal-mark toggle
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

Exercise length is fixed at `GenerateExerciseUseCase.DEFAULT_EXERCISE_LENGTH = 64` (one portrait page: 4 notes × 4 measures × 4 rows) and is no longer user-configurable.
