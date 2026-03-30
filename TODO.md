# TODO

## Immediate
*(none)*


## Features

*(none)*

## Polish / UX

*(none - session-complete screen now implemented)*

## Done (recent)

- [x] **MIDI reconnect flow implemented** - `AndroidMidiManager` now registers a `MidiManager.DeviceCallback`, tracks desired device selection, refreshes available devices reactively, and auto-reconnects when devices are reattached.
- [x] **Statistics tracking + page implemented** - session now records group- and note-level correct/incorrect counters, persists them in DataStore, and exposes a new `Statistics` screen with Top 5 correct/incorrect groups and notes.
- [x] **Exercise-step source typing added** - `ExerciseStep.contentType` now tags generated steps by exercise type so statistics are grouped consistently with settings types (single notes, triads, etc.).
- [x] **Stats/reconnect test coverage added** - tests now cover content-type tagging, session stat accumulation, and top-5 statistics mapping.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **Mixed notehead-color regression fixed** - `GrandStaffCanvas` no longer derives color from the first note in a beat group; each notehead now uses its own state color, so mixed correct/missing/extra note outcomes render correctly in one chord.
- [x] **Stem-color fallback for mixed note states** - stems/flags now use neutral black when a grouped chord contains mixed note states, preventing a single-note state from visually overriding the full chord.
- [x] **Renderer helper tests added** - new `PracticeScreenColorTest` verifies note-state palette mapping and mixed-state stem color behavior.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **Per-note error coloring implemented** - note display now distinguishes matched expected notes (green), missing expected notes (red), and extra played notes (additional yellow noteheads on the same beat/staff).
- [x] **Pedal error coloring split implemented** - expected pedal symbols are evaluated independently (green/red), and unexpected pedal actions now render as additional yellow pedal marks below the staff.
- [x] **Input snapshot tracking added** - `PracticeState` now stores per-beat played input snapshots (`inputByBeat`) so UI rendering uses actual played notes/pedal context instead of blanket beat color.
- [x] **Regression coverage expanded** - added tests for note classification, pedal expectation checks, pedal-only gating, and snapshot persistence/reset behavior.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **Pedal-only events no longer advance steps** - `PracticeSessionUseCase` now treats input with no notes as state-only pedal updates (`MatchResult.Waiting`), so pressing/releasing pedal alone never moves to the next expected note/chord.
- [x] **Pedal press-before-note support** - steps marked with pedal press now validate when the pedal is already held down before the played notes arrive.
- [x] **Pedal release lead-window support** - steps marked with pedal release now validate if release happened on the same input or within 1 second before the played notes.
- [x] **Regression coverage expanded** - new tests verify pedal-only gating plus press-before and release-before behavior in both `MatchNotesUseCaseTest` and `PracticeSessionUseCaseTest`.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **MIDI reopen regression fixed** - `AndroidMidiManager` now closes existing port/device before reopen, rejects stale async open callbacks with request IDs, and invalidates pending opens on `close()`, preventing duplicate receivers that caused all-played-notes to be marked wrong after starting a new exercise.
- [x] **Session reset hardening** - `PracticeViewModel.startSession()` now resets `ChordDetector` before generating/starting the next exercise so pending grouped input cannot leak across exercises.
- [x] **MIDI reopen regression tests added** - new `MidiOpenRequestTrackerTest` verifies request rollover and invalidation behavior used to block stale callback attachment.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **Timeout finish-screen regression fixed** - the practice timer loop now calls `PracticeViewModel.finalizeIfTimedOut(...)`, so the session-complete overlay (score, highscore, correct/wrong) appears even if no new MIDI input arrives after time expires.
- [x] **Timeout boundary tests added** - new `PracticeViewModelTimeoutTest` verifies pre-boundary, boundary, and post-boundary behavior for `hasSessionTimedOut(...)`.
- [x] **Verification rerun** - full `test` passed; debug APK rebuilt, installed, and launched on the VASOUN L10.
- [x] **Note detection regression fixed** - note-only steps no longer fail when incidental pedal CC input is present.
- [x] **Pedal matching guard added** - pedal action is now strictly checked only when a step explicitly expects pedal press/release.
- [x] **Regression test added** - matching now has a unit test for note-only correctness with incidental pedal input.
- [x] **Verification rerun** - full test suite passed; debug APK rebuilt, reinstalled, and relaunched.
- [x] **Scale-consistency fix with accidentals off** - generation now constrains notes to the current major scale when note accidentals are disabled, preventing labels like `Bb4` in C major without a rendered accidental.
- [x] **Exercise length semantics updated** - `exerciseLength` now acts as a max displayed note budget (sum of noteheads), not a fixed number of steps.
- [x] **Timed session mode** - added `exerciseTimeSec` (default 60s) and auto-rollover: when a chunk completes before timeout, a new chunk is generated in the same key.
- [x] **Finish screen stats + persistence** - finish overlay now shows score, highscore, and correct/wrong note counts; highscore and cumulative correct/wrong totals are saved.
- [x] **Regression tests expanded** - unit tests now cover scale-constrained accidental behavior, max-note budget generation, forced-key regeneration, and session counters/rollover state.
- [x] **Repeated device validation screenshots** - captured a multi-run C-major accidental-off validation set: `validation/validation-run1.png` … `validation/validation-run5.png`.
- [x] **Optional generated note accidentals** - note-level accidentals are now gated by settings instead of always appearing for black-key pitches.
- [x] **Generated natural signs** - generated accidentals now include `#`, `b`, and `natural`, with natural signs used only as explicit cancellations after an altered note.
- [x] **Generated pedal press/release events** - exercises can now include timed sustain-pedal marks, and every generated press is paired with a later release.
- [x] **MIDI pedal matching** - sustain pedal CC64 is parsed from MIDI and matched in the same beat-validation flow as notes.
- [x] **Pedal/accidental validation pass** - tests passed and the APK was rebuilt, reinstalled, relaunched, and revalidated on the VASOUN L10.
- [x] **Accidentals on notes** - black-key pitches now render sharp/flat glyphs beside noteheads, with extra accidental columns for close-position chords.
- [x] **Arpeggio exercise generation** - chord-based material can now be generated as broken-chord single-note patterns through the new `ARPEGGIOS` content type.
- [x] **MIDI pedal feasibility verified** - sustain pedal support path was documented as MIDI CC64; current app support remains unimplemented.
- [x] **Fresh accidental/arpeggio validation** - full tests passed, the debug APK was rebuilt, installed on the VASOUN L10, relaunched, and fresh screenshots were saved under `validation/`.
- [x] **Treble clef matched to bass-clef ratio** - the treble clef was reduced to the same scale ratio as the bass clef and revalidated against `mocks/img.png` on device.
- [x] **Treble-clef proportion fix** - the treble clef was reduced from the oversized pass and moved down to better match `mocks/img.png`, then revalidated on-device.
- [x] **Label/clef/key-signature layout fixes (v3)** - clef/key-signature overlap removed, labels split by staff, and label sizing made dynamic to reduce collisions.
- [x] **Notation rendering fixes (v2)** - bass clef dots/baseline corrected, key-signature size increased, and cluster notehead displacement increased to clear the stem.
- [x] **PDF notation review** - `mocks/Standard_music_notation_practice.pdf` was extracted into `validation/pdf-ref/` and used as an additional notation reference.
- [x] **Notation rule tests expanded** - tests now cover clefs, key-signature ordering/proportions, ledger lines, stem direction, notehead displacement, and single-stem chord geometry.
- [x] **Renderer aligned with notation rules** - practice rendering now groups chord noteheads onto a single stem and follows middle-line stem direction rules from the reference PDF.
- [x] **Fresh PDF-based device validation** - the app was rebuilt, deployed, and rechecked on-device with fresh screenshots against the extracted PDF pages.
- [x] **Bass-clef glyph restored** - the broken custom bass-clef body was replaced with a proper glyph renderer while keeping the F-line dot anchors.
- [x] **Validation screenshots organized** - device-validation captures now live under `validation/`.
- [x] **Clustered chords + broader note motion** - exercises now include clustered chord voicings and single-note material with wider leap patterns including fifth-based motion.
- [x] **Exercise type multi-selection** - the old 5-level difficulty model was removed and replaced with multi-select exercise content types.
- [x] **Multi-key selection pool** - settings now support selecting multiple keys and choosing one generated key per exercise.
- [x] **Configurable exercise length** - `AppSettings.exerciseLength` is persisted in DataStore, exposed in Settings, and applied across exercise generation.
