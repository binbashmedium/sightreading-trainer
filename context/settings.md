# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val difficulty: Int = 1,
    val exerciseLength: Int = 8,
    val handMode: HandMode = HandMode.RIGHT,
    val soundEnabled: Boolean = true,
    val musicalKey: Int = 0
)

enum class HandMode { LEFT, RIGHT, BOTH }
```

## Persistence

Settings are persisted using Jetpack DataStore (`Preferences`).

- `SettingsDataStore` maps preference keys to fields, including `musical_key` and `exercise_length`.
- `SettingsRepository` exposes `Flow<AppSettings>` and the save API.
- `SettingsViewModel` and other consumers collect from the repository flow.

## Runtime Usage

### Practice pipeline
`PracticeViewModel` reads settings to configure:
- `AndroidMidiManager.openDevice(settings.midiDeviceName)`
- `ChordDetector(chordWindowMs = settings.chordWindowMs)`
- `PracticeSessionUseCase.processChord(..., timingToleranceMs = settings.timingToleranceMs)`

### Exercise generation
`GenerateExerciseUseCase` uses:
- `difficulty` (1-5)
- `exerciseLength`
- `handMode`
- `musicalKey` (0-11)

## SettingsScreen UI

The screen includes:
- difficulty slider
- exercise-length slider
- key signature chips
- hand-mode chips
- timing tolerance slider
- chord-window slider
- sound toggle
- MIDI device radio list

## UI Notes

`musicalKey` and `handMode` are embedded into `Exercise` at generation time so the practice screen can keep rendering the correct staff assignment and title even if settings change mid-session.
