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

- [x] **Bass-clef glyph restored** - the broken custom bass-clef body was replaced with a proper glyph renderer while keeping the F-line dot anchors.
- [x] **Validation screenshots organized** - device-validation captures now live under `validation/` instead of the repo root.
- [x] **Bass clef geometry anchored** - the bass clef now uses explicit geometry so the dots center around the F line and the top reaches the A line.
- [x] **Clef/key proportion tests** - unit tests now verify bass-clef dot placement, top reach, and key-signature proportions relative to the clefs.
- [x] **Clustered chords + broader note motion** - exercises now include clustered chord voicings and single-note material with wider leap patterns including fifth-based motion.
- [x] **Final verification + deployment** - all tests passed, the debug APK was rebuilt, deployed to the VASOUN L10, and revalidated with a fresh screenshot.
- [x] **Final clef/layout validation pass** - bass clef was lowered and enlarged again, the lead-in gap before the first note was increased, and the updated renderer was revalidated on-device against the `mocks/` references.
- [x] **Bass clef placement** - bass clef baseline offset was corrected so it sits on the proper F anchor line.
- [x] **Exercise type multi-selection** - the old 5-level difficulty model was removed and replaced with multi-select exercise content types.
- [x] **Mixed-type exercise generation** - exercises now combine all selected note/chord types in one session.
- [x] **Chord labels only for actual chords** - single notes show note names, intervals show note pairs, and harmonic labels are reserved for real chords.
- [x] **Regression coverage for exercise-type generation** - tests now cover mixed content selection, non-chord labels, and the revised bass-clef anchor.
- [x] **Correct clef anchors** - treble and bass clef baseline math now uses explicit anchor helpers so the treble curl sits on G and the bass clef anchors to F.
- [x] **Functional chord labels** - rendered chord names now use harmonic labels such as `CM (I)`, `CM7 (Imaj7)`, and `G9 (V9)`.
- [x] **Extended chord generation** - selected exercise types now include sevenths and ninths in addition to single notes, intervals, and triads.
- [x] **Multi-key selection pool** - settings now support selecting multiple keys; each new exercise chooses one generated key from that pool.
- [x] **Regression coverage for keys/chords/clefs** - tests now cover clef baseline helpers, harmonic chord labels, selected-key generation, and richer chord vocabulary.
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
- [x] **Randomized note generation** - generated material is shuffled each call before the selected exercise types are mixed into a session.
- [x] **All 12 musical keys** - selected key pools determine which generated key is used for each exercise; key selection is available in SettingsScreen.
- [x] **Reload button** - single "New Exercise" button replaces Pause + Hint; calls `PracticeViewModel.reloadSession()`.
