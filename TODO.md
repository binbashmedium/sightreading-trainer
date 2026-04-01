# TODO

## Done (recent)

- [x] **exerciseLength setting removed** — `AppSettings.exerciseLength` field removed; exercise length is now fixed at `GenerateExerciseUseCase.DEFAULT_EXERCISE_LENGTH = 64`; `EXERCISE_LENGTH` DataStore key removed; slider removed from `SettingsScreen`; length display removed from `MainScreen`; `exercise_length` string resource removed; `parseExerciseTypes()` signature simplified; test updated to validate `DEFAULT_EXERCISE_LENGTH`.

- [x] **Portrait mode with 4 grand staff rows** — All items completed and tested:
  - Removed `sensorLandscape` orientation lock from `AndroidManifest.xml` → `sensor`
  - Added layout constants: `BEATS_PER_MEASURE_UNITS=8`, `BEATS_PER_ROW=32`, `BEATS_PER_PAGE=128`, `MIN_EXERCISE_NOTES=64`
  - Refactored `GrandStaffCanvas` to accept `startBeat`/`endBeat`/`beatsPerMeasure`/`measureNumberLabel` params
  - Added bar lines (vertical at measure boundaries, thicker at row ends)
  - Portrait: 4 grand-staff rows per page (4 measures × 4 rows), cursor wraps between rows, page flip on last row
  - Landscape: single row, bar lines added
  - Exercise length fixed at 64 (`DEFAULT_EXERCISE_LENGTH`)
  - Measure number label drawn top-left of each portrait row
  - 13 unit tests added in `PortraitLayoutTest` covering all layout helpers and constants

- [x] **Progressions exercise mode added** — `ChordProgression` enum (7 named progressions: I-IV-V-I, I-V-vi-IV, ii-V-I, I-vi-IV-V, I-IV-I-V, vi-IV-I-V, I-iii-IV-V) added to domain model; `PROGRESSIONS` added to `ExerciseContentType`; `GenerateExerciseUseCase` generates progression steps in order (not shuffled) via `buildProgressionSteps()`; `AppSettings.selectedProgressions` persisted in `SettingsDataStore`; progression chip selector shown in `SettingsScreen` when PROGRESSIONS type is active; `HelpScreen` updated with Progressions section and settings entry; 7 new progression tests added to `GenerateExerciseUseCaseTest`.

- [x] **Apache License 2.0 added** — `LICENSE` and `NOTICE` files created; Apache 2.0 header prepended to all 39 Kotlin source files.
- [x] **F-Droid eligibility verified** — app has no proprietary deps, no Firebase/Play Services, no tracking/ads; qualifies for F-Droid inclusion under Apache 2.0.
- [x] **F-Droid fastlane metadata added** — `fastlane/metadata/android/` created with English and German `title.txt`, `short_description.txt`, `full_description.txt`, and `changelogs/1.txt`.
- [x] **MIDI reconnect flow implemented** — `AndroidMidiManager` now registers a `MidiManager.DeviceCallback`, tracks desired device selection, refreshes available devices reactively, and auto-reconnects when devices are reattached.
- [x] **Statistics tracking + page implemented** — session now records group- and note-level correct/incorrect counters, persists them in DataStore, and exposes a new `Statistics` screen with Top 5 correct/incorrect groups and notes.
- [x] **Exercise-step source typing added** — `ExerciseStep.contentType` now tags generated steps by exercise type so statistics are grouped consistently with settings types (single notes, triads, etc.).
- [x] **Mixed notehead-color regression fixed** — each notehead now uses its own state color; mixed correct/missing/extra note outcomes render correctly in one chord.
- [x] **Per-note error coloring implemented** — matched expected notes (green), missing expected notes (red), extra played notes (additional yellow noteheads on the same beat/staff).
- [x] **Pedal error coloring split implemented** — expected pedal symbols evaluated independently (green/red); unexpected pedal actions render as additional yellow pedal marks.
- [x] **Input snapshot tracking added** — `PracticeState.inputByBeat` stores per-beat played input snapshots.
- [x] **Pedal-only events no longer advance steps** — `PracticeSessionUseCase` treats input with no notes as state-only pedal updates (`MatchResult.Waiting`).
- [x] **MIDI reopen regression fixed** — `AndroidMidiManager` now closes existing port/device before reopen, rejects stale async open callbacks with request IDs.
- [x] **Timeout finish-screen regression fixed** — practice timer loop calls `PracticeViewModel.finalizeIfTimedOut(...)` so the session-complete overlay appears even with no MIDI input after time expires.
- [x] **Scale-consistency fix with accidentals off** — generation constrains notes to the current major scale when note accidentals are disabled.
- [x] **Timed session mode** — added `exerciseTimeSec` (default 60s) and auto-rollover: when a chunk completes before timeout, a new chunk is generated in the same key.
- [x] **Optional generated note accidentals** — note-level accidentals gated by settings; generated accidentals include `#`, `b`, and `natural` cancellations.
- [x] **Generated pedal press/release events** — exercises can include timed sustain-pedal marks; every generated press is paired with a later release.
- [x] **MIDI pedal matching** — sustain pedal CC64 parsed from MIDI and matched in the same beat-validation flow as notes.
- [x] **Accidentals on notes** — black-key pitches render sharp/flat glyphs beside noteheads, with staggered columns for close-position chords.
- [x] **Arpeggio exercise generation** — chord-based material can be generated as broken-chord single-note patterns (`ARPEGGIOS` content type).
- [x] **Renderer aligned with notation rules** — chord noteheads grouped onto a single stem; middle-line stem direction rules from PDF reference; clustered chords, broader note motion.
- [x] **Exercise type multi-selection** — old 5-level difficulty model replaced with multi-select exercise content types.
- [x] **Multi-key selection pool** — settings support selecting multiple keys; one generated key chosen per exercise.
