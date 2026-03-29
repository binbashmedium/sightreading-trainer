# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val difficulty: Int = 1,
    val handMode: HandMode = HandMode.RIGHT,
    val soundEnabled: Boolean = true
)

enum class HandMode { LEFT, RIGHT, BOTH }
```

## Persistence

Settings are persisted using Jetpack DataStore (`Preferences`).

- `SettingsDataStore` maps preference keys ↔ fields
- `SettingsRepository` exposes `Flow<AppSettings>` and update API
- `SettingsViewModel` and other consumers collect from repository flow

## Runtime Usage

### Practice pipeline
`PracticeViewModel` collects settings to configure:
- `AndroidMidiManager.openDevice(settings.midiDeviceName)`
- `ChordDetector(chordWindowMs = settings.chordWindowMs)`
- `PracticeSessionUseCase.processChord(..., timingToleranceMs = settings.timingToleranceMs)`

### Exercise generation
`ExerciseRepository` and `GenerateExerciseUseCase` use:
- `difficulty`
- `handMode`

## UI Notes

The new grand-staff UI is fully state-driven and does not introduce additional persisted settings.
`Pause` and `Hint` controls in `PracticeScreen` are presentation-level controls currently handled in the UI layer.
