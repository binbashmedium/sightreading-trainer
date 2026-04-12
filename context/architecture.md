# Architecture

## Overview

The project uses a two-module clean split:

- `:domain` — pure Kotlin/JVM business logic (no Android dependencies).
- `:app` — Android/Compose layer, MIDI I/O, persistence, DI, and UI.

Practice rendering is driven by domain state mapped into UI models, then rendered with **Verovio** (`VerovioStaffView`) instead of the legacy canvas renderer.

## Module Structure

### `:domain`

- `model/` — `Exercise`, `ExerciseStep`, `PracticeState`, `AppSettings`, `NoteValue`, `MatchResult`, `StepInputSnapshot`.
- `usecase/` — `GenerateExerciseUseCase`, `MatchNotesUseCase`, `PracticeSessionUseCase`.
- `core/midi/` — `ChordDetector` groups note + pedal input into chord windows.
- `core/util/` — note-name helpers.

### `:app`

- `core/midi/AndroidMidiManager` — Android MIDI device discovery/open/reconnect and message parsing.
- `data/SettingsDataStore` + `SettingsRepository` — DataStore-backed settings flow.
- `data/ExerciseRepository` + `ExerciseSource` — selectable source (`GENERATED` or `DATABASE`) with generated fallback.
- `ui/PracticeViewModel` — session orchestration, timeout finalization, loading lifecycle.
- `ui/PracticeScreen` — orientation-aware layout + loading/complete overlays.
- `ui/VerovioStaffView` + `ui/MeiConverter` — notation rendering pipeline.
- `ui/GrandStaffModels` — Android-free render math + chord-label detection helpers.

## Runtime Data Flow

1. Exercise is requested from `ExerciseRepository` based on `AppSettings.exerciseInputSource`.
2. `PracticeViewModel` starts/resets session state via `PracticeSessionUseCase`.
3. MIDI events flow through `AndroidMidiManager` and are grouped by `ChordDetector`.
4. Grouped input is matched against current `ExerciseStep` by `PracticeSessionUseCase` (`MatchNotesUseCase`).
5. `PracticeScreen.toGameState(...)` maps domain state to UI `GameState`.
6. `VerovioStaffView` renders MEI output from `MeiConverter` and reports first-render callback.

## Practice Layout

- Portrait: 4 rows per page, each row = 4 measures.
- Landscape: single 4-measure row window.
- `BEATS_PER_STEP = 2f`; note-value width is cumulative via `NoteValue.uiBeatUnits`.

The loading overlay is cleared only after Verovio first-render callback (`onStaffRendered`) to avoid race conditions.

## Dependency Injection

Hilt (`SingletonComponent`) provides app-scoped dependencies such as MIDI manager, repositories, and use cases.

## Navigation

Single-activity Compose navigation routes:
- `main`
- `practice`
- `settings`
- `statistics`
