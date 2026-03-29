# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",          // name of the selected MIDI device
    val timingToleranceMs: Int = 200,         // ms window for Correct vs TooEarly/TooLate
    val chordWindowMs: Int = 50,              // ms window for grouping notes into a chord
    val difficulty: Int = 1,                  // 1–5, maps to GenerateExerciseUseCase levels
    val handMode: HandMode = HandMode.RIGHT,  // LEFT | RIGHT | BOTH
    val soundEnabled: Boolean = true          // reserved for future audio feedback
)

enum class HandMode { LEFT, RIGHT, BOTH }
```

## Persistence — DataStore

Settings are stored with **Jetpack DataStore (Preferences)**. No schema migration is needed since DataStore<Preferences> uses string keys.

### SettingsDataStore (`app/data/SettingsDataStore.kt`)

Defines `Preferences.Key` constants for each field and handles serialisation:
- `HandMode` is stored as its `name` string, parsed back with `HandMode.valueOf()`
- Numeric fields stored as `Int`
- Boolean fields as `Boolean`

### SettingsRepository (`app/data/SettingsRepository.kt`)

Exposes a single `Flow<AppSettings>` that emits whenever any setting changes.
Provides `suspend fun update(transform: (AppSettings) -> AppSettings)` for writes.

### Data Flow

```
DataStore<Preferences>
        │  Flow<Preferences>
        ▼
SettingsDataStore.settingsFlow   (maps Preferences → AppSettings)
        │  Flow<AppSettings>
        ▼
SettingsRepository.settings
        │  collectAsState()
        ▼
SettingsViewModel.uiState        (StateFlow<AppSettings>)
        │
        ▼
SettingsScreen                   (sliders, dropdowns, toggles)
```

## Settings Screen

`SettingsScreen` renders controls for all settings:

| Setting | Control | Range / Values |
|---|---|---|
| MIDI device | Dropdown | List from `AndroidMidiManager.getDeviceNames()` |
| Timing tolerance | Slider | 50–500 ms |
| Chord window | Slider | 20–200 ms |
| Difficulty | Slider | 1–5 |
| Hand mode | Segmented button | LEFT / RIGHT / BOTH |
| Sound | Toggle | on / off |

Changes are written to DataStore immediately on user interaction (no "Save" button needed).

## Usage in Other Components

- `PracticeViewModel` collects `SettingsRepository.settings` to pass `timingToleranceMs` and `chordWindowMs` to `ChordDetector` and `MatchNotesUseCase`
- `ExerciseRepository` calls `GenerateExerciseUseCase.execute(settings)` with the current settings snapshot when starting a new session
- `AndroidMidiManager.openDevice(settings.midiDeviceName)` is called when the practice session starts
