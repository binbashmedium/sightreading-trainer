# TODO

## In Progress

- [x] **Google Play Release Bundle** — Dokumentation und Release-Workflow für ein signiertes Android App Bundle (`.aab`) ergänzt: `README.md` enthält `bundleRelease` inkl. Output-Pfad und Keystore-Umgebungsvariablen; `fastlane/Fastfile` enthält Lane `build_release_bundle`.
- [x] **GitHub Actions Release Bundle** — Manueller Workflow `.github/workflows/android-release-bundle.yml` ergänzt (Restore von `KEYSTORE_BASE64` nach `/tmp/upload-keystore.jks`, `bundleRelease` mit Signing-Secrets, Upload von `app-release.aab` als Artifact) und README um Secret-/Setup-Anleitung erweitert.

## Done (recent)

- [x] **Single-note run chord-name display fix** — `resolveDisplayChordNotes()` groups consecutive single-note steps into runs and resolves them to a detected chord when possible (e.g. C-E-G displays `CM`). 4 unit tests added: run grouping, passthrough, split boundaries, isolated-melody fallback.
- [x] **Suspended chord label detection** — `CHORD_QUALITIES` extended with `sus2` and `sus4`; `CHORD_QUALITY_PRIORITY` map ensures `Gsus2` is preferred over `Dsus4` for D-G-A. Tests added.
- [x] **Extended chord quality detection** — Added `5`, `aug`, `dim7`, `6`, `m6`, `add9`, `madd9`, `M11`/`11`/`m11`, `M13`/`13`/`m13`, `7b9`, `7#9`, `7#11`, `7b13` to `CHORD_QUALITIES`. Regression tests added.
- [x] **Key-aware enharmonic chord roots** — `pitchClassNameForKey()` uses `SHARP_KEY_NAMES`/`FLAT_KEY_NAMES` based on the active key signature; `formatChordLabel()` and `formatChordLabelShort()` use it. Tests added.
- [x] **Measure-level chord detector for display** — `buildMeasureChordLabels()` collects all notes per bar and emits one `Chord` per measure with exact or superset detection. Tests added.
- [x] **No note-name fallback for unresolved bars** — `buildMeasureChordLabels()` omits measures where no chord can be detected; regression test added.
- [x] **Windowed-probe fallback in measure labeling** — `buildMeasureChordLabels()` now falls back to `resolveDisplayChordNotes` on the measure's single-note steps when `detectChord` and `detectChordSuperset` both fail; catches power-chord pairs embedded in chromatic passages (e.g. C-G-Db-Ab → C#5). Unit test added.
- [x] **Stable suspended-voicing naming** — `detectChord()` now collects all candidate matches and picks the highest-priority quality, making sus2/sus4 disambiguation deterministic. Test added.

- [x] **Loading Screen** — `PracticeViewModel.isLoading: StateFlow<Boolean>` added; set to true in `startSession()`, false after exercise is ready. `VerovioStaffView` exposes `onFirstRender` callback; `PracticeViewModel.onStaffRendered()` resets session start time once Verovio renders. `PracticeScreen` shows `CircularProgressIndicator` overlay when loading. Timer loop only runs when not loading.
- [x] **Settings link from Exercise screen** — Gear icon `IconButton` (using `Icons.Filled.Settings`) added to `HeaderCard`; navigates to Settings screen. `material-icons-extended` dependency added.
- [x] **App stops responding / reset fix** — `startSession()` now clears `_sessionResult` and calls `practiceSessionUseCase.resetSession()` before generating the next exercise, ensuring BPM/score state is cleared immediately on reload.
- [x] **Remove points; redesign highscore** — `GameState.score` removed; `SessionResultUi.score` replaced with `accuracy: Int`, `bpm`, `practiceTimeSec`. `computeHighScore()` computes composite score from accuracy × BPM-factor × time-factor. Overlay now shows accuracy %, BPM, time and highscore.
- [x] **Pedal mark alignment** — `applyPedalMarks` now builds cumulative beat positions and only places press/release at integer quarter-note beat boundaries. Release is ≥ 2 beats and ≤ 6 beats after press. 3 new unit tests added.
- [x] **Natural accidental display fix** — `MeiConverter.visualAccidAttr` suppresses natural signs unless the pitch class was altered by the key signature or by an explicit accidental earlier in the same measure. `keySignatureAlteredPitchClasses()` helper computes the set of affected pitch classes. `renderLayer` tracks within-measure accidentals. 7 new unit tests added.
- [x] **Configurable note range** — `AppSettings` gains `bassNoteRangeMin`/`Max` (MIDI 28–72) and `trebleNoteRangeMin`/`Max` (MIDI 48–93); `GenerateExerciseUseCase.applyNoteRanges()` octave-shifts then clamps generated notes; `SettingsDataStore` persists new fields; sliders added to `SettingsScreen`. 3 unit tests added.
- [x] **Ornaments** — `OrnamentType` enum (NONE, TRILL, MORDENT, TURN) added to domain; `ExerciseStep.ornament` field; `GenerateExerciseUseCase.applyOrnaments()` randomly assigns ornaments (~1-in-6 on quarter-note+ steps) when `ornamentsEnabled`; `NoteEvent.ornament` carries it to the UI; `MeiConverter` emits `<trill>`/`<mordent>`/`<turn>` control events with `startid` references; notes still evaluated on main pitch only; `ornamentsEnabled` toggle in `SettingsScreen`. 7 unit tests added.
- [x] **Configurable ornament types** — `ornamentsEnabled: Boolean` replaced by `selectedOrnaments: Set<OrnamentType>` in `AppSettings`; each ornament type (TRILL, MORDENT, TURN) individually selectable via FilterChips in `SettingsScreen`; empty set means no ornaments (same pattern as chord types and note values); `SettingsDataStore` serializes set as comma-separated string; `applyOrnaments()` in `GenerateExerciseUseCase` picks only from selected types; `HelpScreen` updated with Ornaments subsection; ornament unit tests updated for new API.
- [x] **Extended ornaments — upper mordent and grace notes** — Added `UPPER_MORDENT` (inverted mordent, rendered as `<mordent form="upper"/>`) and `GRACE_NOTE` (acciaccatura, rendered as inline `<note grace="acc"/>` one semitone below main note) to `OrnamentType`; `MORDENT` now explicitly emits `form="lower"`; `SettingsScreen` shows all 5 types with human-readable labels ("Lower Mordent", "Upper Mordent", "Grace Note"); `HelpScreen` updated with per-type descriptions; 5 new `MeiConverterTest` tests added.
- [x] **Full ornament set — appoggiatura, arpeggiation, renamed acciaccatura** — `GRACE_NOTE` renamed to `ACCIACCATURA`; `APPOGGIATURA` added (inline `<note grace="unacc"/>` one semitone above), `ARPEGGIATION` added (`<arpeg tstamp=".." staff=".."/>` per beat/staff group, only applied to chords by `applyOrnaments()`); `SettingsScreen` shows all 7 types; `HelpScreen` updated with how-to-play descriptions for each ornament; 7 new `MeiConverterTest` tests.

      

## Done (recent)

- [x] **Thin cursor line + pedal mark rendering** — `staff.html`: cursor `stroke-width` changed from fixed 30 to proportional (0.5% of viewBox width, min 2) for a slim line at any screen size. `MeiConverter`: pedal marks now converted to MEI `<pedal tstamp="..." staff="2" dir="down/up" [color="..."]/>` control events; `Locale.US` used for tstamp formatting to avoid locale-dependent decimal separators. 7 new `MeiConverterTest` pedal tests added (141 total pass).

- [x] **Rotation stability + full-width staff** — `LaunchedEffect` now only calls `startSession()` when `practiceState == null` (rotation no longer regenerates notes); `staff.html` uses correct Verovio options (`svgViewBox: true`, `adjustPageWidth: true`, `breaks: "none"`) so SVG is emitted with a viewBox and CSS `width: 100%; height: auto` gives fully responsive scaling; `constrainSvgHeight()` reduces width if viewBox aspect ratio would overflow portrait row height; WebView cache cleared on creation (`LOAD_NO_CACHE` + `clearCache(true)`).

- [x] **Verovio integration** — Replaced custom Canvas staff rendering with Verovio (music notation library). `GrandStaffCanvas` replaced by `VerovioStaffView` (WebView + Verovio.js v6.1.0 bundled in assets); `MeiConverter` converts `GameState` → MEI XML; note state coloring (CORRECT/WRONG/LATE) via MEI `@color`; cursor via JavaScript SVG injection (`drawCursor()` finds `ncurr*` xml:ids); portrait 4-row and landscape 1-row layout preserved; `MeiConverterTest` (30+ unit tests) covers pitch mapping, duration, key signature, note coloring, chord grouping, beat range filtering, natural accidentals.

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
