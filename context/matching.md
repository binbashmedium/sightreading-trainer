# Note Matching

## MatchNotesUseCase (`domain/usecase/MatchNotesUseCase.kt`)

Stateless use-case called for each detected chord event.

### Signature
```kotlin
fun execute(
    playedNotes: List<NoteEvent>,
    expectedNotes: List<Int>,
    toleranceMs: Long = 200L,
    expectedTimeMs: Long? = null
): MatchResult
```

### Matching Logic

1. **Pitch matching (strict)**
   - Extract MIDI note numbers from played notes
   - Compare sorted lists with expected notes
   - Mismatch → `MatchResult.Incorrect`

2. **Timing check (optional)**
   - Runs only when `expectedTimeMs` is provided
   - `delta = playedNotes[0].timestamp - expectedTimeMs`
   - `delta < -toleranceMs` → `TooEarly`
   - `delta > toleranceMs` → `TooLate`
   - otherwise → `Correct`

## MatchResult States

| State | Meaning |
|---|---|
| `Correct` | Notes match, timing within tolerance |
| `Incorrect` | Wrong/missing/extra pitch set |
| `TooEarly` | Correct pitch set but too soon |
| `TooLate` | Correct pitch set but too late |
| `Waiting` | Initial/idle state |

## Practice Session Wrapper

`PracticeSessionUseCase` stores session progress and updates `PracticeState` after each chord:

- **Score**: base **+10 pts** per correct chord; **fluency bonus** of up to +10 pts for fast
  consecutive playing (`max(0, (2000 - interChordMs) / 200)` integer pts).
- **BPM**: calculated from last inter-chord interval: `60000 / interChordMs`, capped at 300.
  Updated only on correct chords. Shown as `PracticeState.bpm`.
- **Per-beat results**: `PracticeState.resultByBeat: Map<Int, MatchResult>` stores the latest
  result for each chord index — used by the UI for accurate note colouring per beat.

## UI Visualization Mapping

`PracticeScreen.toGameState` maps `resultByBeat[beatIndex]` to `NoteState`:
- `Correct`   → green (`NoteState.CORRECT`, `#2E7D32`)
- `Incorrect` → red (`NoteState.WRONG`, `#C62828`)
- `TooLate`   → yellow (`NoteState.LATE`, `#F9A825`)
- pending/unplayed → black (`NoteState.NONE`)

## Cursor Behaviour

The red time cursor sits **statically** at `exercise.currentIndex * 2f` beats. It advances
only when the user plays the correct notes — the app waits for input rather than advancing
on wall-clock time.

## Future Matching Modes

Only strict equality is currently implemented. A tolerant mode (`expected ⊆ played`) can be
introduced by extending `MatchNotesUseCase` and the UI mapper.
