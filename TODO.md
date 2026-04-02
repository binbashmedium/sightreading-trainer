# TODO

## In Progress

(none)

## Done (recent)

- [x] **Note type selection + bar-line gap** — Added `selectedNoteValues: Set<NoteValue>` to `AppSettings`; settings UI shows Whole/Half/Quarter/Eighth FilterChips (at least one required); `GenerateExerciseUseCase.applyMeasurePatterns` filters `MEASURE_PATTERNS` to selected note values with a bar-line gap constraint (`BARLINE_GAP_BEATS = 1f`): last notehead per measure must start ≤ 3 beats from the bar line (excludes 8×EIGHTH pattern; fallback to gap-valid patterns); `BAR_LINE_SHIFT_RATIO` increased from 0.65 to 1.5; `SettingsDataStore` persists the new field; 86 domain tests pass.

- [x] **Bar-line/notehead overlap fix (final)** — `BAR_LINE_SHIFT_RATIO` increased from 0.35 to 0.65 (must exceed notehead radius ratio of 0.55) so the bar line edge clears the notehead circle of the following measure's first note. `beatWidth` minimum changed from hard-coded `20f` to `lineSpacing * 0.5f` to prevent C# key (7 sharps) notes from overflowing the canvas in portrait mode. Two regression tests added: `bar line shift clears notehead radius` and `beatWidth floor uses lineSpacing fraction not fixed pixel minimum`; all 88 app tests pass; verified on VASOUN L10 with C, C#, G, Bb, B keys across 5+ exercise regenerations.

- [x] **Portrait crash fix** — `GrandStaffCanvas` crashed on portrait rows (`IllegalArgumentException: Cannot coerce value to an empty range`) when `lineSpacing * 0.42f < 10f` (portrait rows are ~270 px tall → lineSpacing ≈ 15 px → max 6.3 px < min 10 px). Fixed by computing `maxLabelSize = lineSpacing * 0.42f` and using `coerceIn(minOf(10f, maxLabelSize), maxLabelSize)`; regression test added to `PortraitLayoutTest`; all 66 tests pass; APK rebuilt and verified on VASOUN L10 in both landscape (4-measure single row) and portrait (4-row page) modes.

- [x] **Eighth note beaming (Ligatures)** — Consecutive eighth notes grouped per quarter beat (2 per beat) are connected by a horizontal beam bar instead of individual flags; `BeamGroup` data class and `computeBeamGroups()` added to `GrandStaffModels.kt`; two-pass rendering in `GrandStaffCanvas`: pass 1 collects natural stem end-Y for beamed positions, computes shared beam Y (most extreme), pass 2 uses unified stem direction and extends all stems in group to beam Y, skips flags for beamed notes, draws beam bar after loop (`strokeWidth = lineSpacing * 0.5f`); 12 `BeamGroupTest` unit tests added; all 83 tests pass (44 domain + 39 app).

- [x] **Mixed note values (whole, half, quarter, eighth)** — 4/4 time stays; landscape shows 4 measures per view; portrait shows 16 measures (4 rows × 4 measures); per measure exactly 1 whole OR 2 halves OR 4 quarters OR 8 eighths (uniform per measure, randomly selected); `ExerciseStep.noteValue: NoteValue` added; `DEFAULT_EXERCISE_MEASURES = 16` and `MATERIAL_POOL_SIZE = 128` replace `DEFAULT_EXERCISE_LENGTH`; `NoteValue.uiBeatUnits` extension in `GrandStaffModels`; cumulative beat positions in `toGameState()`; `duration = step.noteValue.beats`; landscape 4-measure pagination; 44 domain tests pass, 27 app unit tests pass (13 `PortraitLayoutTest` + 14 `NoteValueLayoutTest`).

- [x] **exerciseLength setting removed** — `AppSettings.exerciseLength` field removed; exercise length is now fixed at `GenerateExerciseUseCase.DEFAULT_EXERCISE_MEASURES = 16`; `EXERCISE_LENGTH` DataStore key removed; slider removed from `SettingsScreen`; length display removed from `MainScreen`; `exercise_length` string resource removed; `parseExerciseTypes()` signature simplified.

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
