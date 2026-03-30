# TODO

## Immediate

*(none)*

## Features

- [ ] **MIDI reconnect flow** - `AndroidMidiManager` has no disconnect/reconnect handling. Device loss silently breaks the session.
- [ ] **Multi-port MIDI** - only port index 0 is opened. Support devices that expose notes on other ports.
- [ ] **MIDI channel to hand assignment** - channel information is parsed but ignored. Could drive `HandMode` automatically (ch 1 = right, ch 2 = left).
- [ ] **Tolerant pitch matching** - only strict equality is implemented. A future `expected subset played` mode would allow extra notes.
- [ ] **Accidentals on notes** - the staff position formula uses diatonic steps so sharps/flats share the line/space of their natural note. A small sharp/flat glyph to the left of the notehead is not yet rendered.

## Polish / UX

*(none - session-complete screen now implemented)*

## Done (recent)

- [x] **Clef placement/layout** - staff lines now extend beneath the clefs; treble and bass clefs are anchored to the G4 and F3 staff lines with dedicated left-side spacing before notes begin.
- [x] **Configurable exercise length** - `AppSettings.exerciseLength` is persisted in DataStore, exposed in Settings, and applied across exercise generation.
- [x] **Hand-aware exercise generation** - left-hand exercises now render on the bass staff, and both-hand exercises generate material that spans both staves.
- [x] **Regression coverage for layout/generation** - tests now cover staff anchor helpers, hand-aware staff assignment, configurable exercise length, and both-hand generation.
- [x] **Verification + deployment** - tests passed, debug APK built, and the app was installed/launched on the VASOUN L10.
- [x] **Grand staff diatonic placement** - `midiToGrandStaffY` now uses diatonic step arithmetic (E4=treble bottom, G2=bass bottom) instead of a chromatic semitone formula. Notes land on the correct lines/spaces.
- [x] **Ledger lines** - `ledgerStepsBelow` / `ledgerStepsAbove` helpers compute which lines to draw; `GrandStaffCanvas` draws them centered on each notehead.
- [x] **Key signature rendering** - sharps/flats drawn after each clef on both treble and bass staves using standard accidental-order arrays.
- [x] **Staff assignment by pitch** - notes >= MIDI 60 rendered on treble, notes < 60 on bass. Right-hand exercises on upper staff, left-hand on lower staff.
- [x] **Wrong note advances cursor** - `PracticeSessionUseCase` always increments `currentIndex`, so any played chord (right or wrong) moves the exercise forward.
- [x] **Session complete screen** - overlay shown when `exercise.isComplete`; displays final score, BPM, and a "New Exercise" button.
- [x] **Commit `NoteValue.kt`** - `domain/model/NoteValue.kt` is now tracked.
- [x] **Lock to landscape** - `android:screenOrientation="sensorLandscape"` in `AndroidManifest.xml`.
- [x] **Per-beat match coloring** - `PracticeState.resultByBeat: Map<Int, MatchResult>` stores the result per chord index; `toGameState` reads it for accurate colouring.
- [x] **Static cursor (input-driven)** - cursor is fixed at `exercise.currentIndex * 2f`; advances only when a chord is played.
- [x] **BPM display + fluency scoring** - BPM calculated from inter-chord timing; fluency bonus of up to +10 pts added to the base +10 pts per correct chord.
- [x] **Randomized note generation** - all levels shuffle notes each call via `kotlin.random.Random`.
- [x] **All 12 musical keys** - `AppSettings.musicalKey` (0-11) transposes all exercises; key selector added to SettingsScreen; key name shown in practice header.
- [x] **Reload button** - single "New Exercise" button replaces Pause + Hint; calls `PracticeViewModel.reloadSession()`.
