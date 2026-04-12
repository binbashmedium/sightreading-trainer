# Settings

## AppSettings

`AppSettings` (domain) persists runtime + generation configuration, including:

- MIDI: `midiDeviceName`, `timingToleranceMs`, `chordWindowMs`
- Session: `exerciseTimeMin`, `soundEnabled`, `highScore`
- Generation mode: `exerciseMode` (`CLASSIC`/`PROGRESSIONS`)
- Material selection: `exerciseTypes`, `selectedProgressions`, `progressionExerciseTypes`
- Theory controls: `selectedKeys`, `selectedScaleType`, `selectedNoteValues`
- Performance notation generation: `noteAccidentalsEnabled`, `pedalEventsEnabled`, `selectedOrnaments`
- Staff range clamps: bass/treble min/max MIDI values
- Stats persistence maps (`correct/wrong` for groups + notes)
- Input source: `exerciseInputSource` (`GENERATED`/`DATABASE`)

## Persistence

`SettingsDataStore` serializes all settings into DataStore `Preferences`.
`SettingsRepository` exposes a `Flow<AppSettings>` used by all relevant view models.

## Runtime Consumers

- `PracticeViewModel`
  - opens MIDI device using configured name
  - applies chord window and timing tolerance
  - controls session timing/finalization
- `GenerateExerciseUseCase`
  - consumes mode/type/key/scale/range/note-value/ornament/pedal settings
- `ExerciseRepository`
  - selects source via `exerciseInputSource`

## Settings UI

`SettingsScreen` includes:
- source selection (`Generated` / `Database`)
- mode selection (Mode 1 classic / Mode 2 progressions)
- type chips, progression chips, progression voicing chips
- key chips + scale chips
- hand-mode chips
- note values chips (at least one)
- toggles for chord names, generated accidentals, generated pedal events, sound
- ornament chips
- bass/treble range sliders
- tolerance/window sliders
- MIDI device radio list

## Notes

Exercise length is fixed (16 measures); it is no longer user-configurable.
