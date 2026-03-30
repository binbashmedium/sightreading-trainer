# TODO

## Immediate

- [ ] **Commit `NoteValue.kt`** — `domain/model/NoteValue.kt` is untracked (rhythmic duration enum: WHOLE/HALF/QUARTER/EIGHTH). Needs to be staged and committed.
- [ ] **Wire `NoteValue` into exercise generation** — `toGameState` still uses hardcoded `duration = 1f` for all notes. Pair `NoteValue` with each chord so rendered glyphs match actual durations.

## Features

- [ ] **MIDI reconnect flow** — `AndroidMidiManager` has no disconnect/reconnect handling. Device loss silently breaks the session.
- [ ] **Multi-port MIDI** — only port index 0 is opened. Support devices that expose notes on other ports.
- [ ] **MIDI channel → hand assignment** — channel information is parsed but ignored. Could drive `HandMode` automatically (ch 1 = right, ch 2 = left).
- [ ] **Tolerant pitch matching** — only strict equality is implemented. A future `expected ⊆ played` mode would allow extra notes.

## Polish / UX

- [ ] **Session complete screen** — when `exercise.isComplete` there is no end-of-session feedback or navigation. Should show final score, BPM, and a replay button.
- [ ] **Accidental wrong-note rendering** — `toGameState` uses `currentChord.first() + 1` as a placeholder wrong note. Should display the actual played MIDI notes from the last incorrect attempt.

## Done (recent)

- [x] **Lock to landscape** — `android:screenOrientation="sensorLandscape"` in `AndroidManifest.xml`.
- [x] **Per-beat match coloring** — `PracticeState.resultByBeat: Map<Int, MatchResult>` stores the result per chord index; `toGameState` reads it for accurate colouring.
- [x] **Static cursor (input-driven)** — cursor is fixed at `exercise.currentIndex * 2f`; advances only when user plays the correct notes.
- [x] **BPM display + fluency scoring** — BPM calculated from inter-chord timing; fluency bonus of up to +10 pts added to the base +10 pts per correct chord.
- [x] **Randomized note generation** — all levels shuffle notes each call via `kotlin.random.Random`.
- [x] **All 12 musical keys** — `AppSettings.musicalKey` (0–11) transposes all exercises; key selector added to SettingsScreen; key name shown in practice header.
- [x] **Reload button** — single "New Exercise" button replaces Pause + Hint; calls `PracticeViewModel.reloadSession()`.
