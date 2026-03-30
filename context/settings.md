# Settings

## AppSettings Model (`domain/model/AppSettings.kt`)

```kotlin
data class AppSettings(
    val midiDeviceName: String = "",
    val timingToleranceMs: Int = 200,
    val chordWindowMs: Int = 50,
    val difficulty: Int = 1,
    val handMode: HandMode = HandMode.RIGHT,
    val soundEnabled: Boolean = true,
    val musicalKey: Int = 0   // 0=C, 1=C#/Db, …, 11=B
)

enum class HandMode { LEFT, RIGHT, BOTH }
```

## Persistence

Settings are persisted using Jetpack DataStore (`Preferences`).

- `SettingsDataStore` maps preference keys ↔ fields (includes `musical_key` int key)
- `SettingsRepository` exposes `Flow<AppSettings>` and update API
- `SettingsViewModel` and other consumers collect from repository flow

## Runtime Usage

### Practice pipeline
`PracticeViewModel` reads settings to configure:
- `AndroidMidiManager.openDevice(settings.midiDeviceName)`
- `ChordDetector(chordWindowMs = settings.chordWindowMs)`
- `PracticeSessionUseCase.processChord(..., timingToleranceMs = settings.timingToleranceMs)`

### Exercise generation
`GenerateExerciseUseCase` uses:
- `difficulty` (1–5)
- `handMode`
- `musicalKey` (0–11) — transposes all notes by this semitone offset from C

## SettingsScreen UI

Key signature is selected via a horizontally-scrollable `LazyRow` of `FilterChip`s showing
all 12 key names. Other controls (Difficulty slider, Hand mode chips, Tolerance slider,
Chord window slider, Sound toggle, MIDI device radio) are unchanged.

## UI Notes

The grand-staff UI is fully state-driven. The `musicalKey` is embedded in `Exercise.musicalKey`
at generation time so the level title can show the key even when settings change mid-session.
