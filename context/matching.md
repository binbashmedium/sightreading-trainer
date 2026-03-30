# Note Matching

## MatchNotesUseCase (`domain/usecase/MatchNotesUseCase.kt`)

Stateless use-case called for each grouped performance input.

### Signature
```kotlin
fun execute(
    playedNotes: List<NoteEvent>,
    playedPedalAction: PedalAction,
    expectedStep: ExerciseStep,
    toleranceMs: Long = 200L,
    expectedTimeMs: Long? = null
): MatchResult
```

### Matching Logic

1. **Pitch matching (strict)**
   - Extract MIDI note numbers from played notes
   - Compare sorted lists with `expectedStep.notes`
   - Mismatch returns `MatchResult.Incorrect`

2. **Pedal matching**
   - Compare `playedPedalAction` with `expectedStep.pedalAction`
   - Mismatch returns `MatchResult.Incorrect`

3. **Timing check (optional)**
   - Runs only when `expectedTimeMs` is provided
   - `delta = playedNotes[0].timestamp - expectedTimeMs`
   - `delta < -toleranceMs` -> `TooEarly`
   - `delta > toleranceMs` -> `TooLate`
   - otherwise -> `Correct`

## MatchResult States

| State | Meaning |
|---|---|
| `Correct` | Notes/pedal match, timing within tolerance |
| `Incorrect` | Wrong/missing/extra pitch set or pedal mismatch |
| `TooEarly` | Correct input but too soon |
| `TooLate` | Correct input but too late |
| `Waiting` | Initial/idle state |

## Practice Session Wrapper

`PracticeSessionUseCase` stores session progress and updates `PracticeState` after each step:

- **Score**: base +10 pts per correct step; fluency bonus up to +10 for fast consecutive correct steps.
- **BPM**: calculated from last inter-correct-step interval (`60000 / interStepMs`, capped at 300).
- **Per-beat results**: `resultByBeat: Map<Int, MatchResult>` stores latest result for each step index.
- **Note counters**: `correctNotesCount` and `wrongNotesCount` accumulate expected note counts per matched step.

## UI Visualization Mapping

`PracticeScreen.toGameState` maps `resultByBeat[beatIndex]` to `NoteState`:
- `Correct` -> green (`NoteState.CORRECT`, `#2E7D32`)
- `Incorrect` -> red (`NoteState.WRONG`, `#C62828`)
- `TooLate` -> yellow (`NoteState.LATE`, `#F9A825`)
- pending/unplayed -> black (`NoteState.NONE`)

## Cursor Behavior

The red cursor sits at `exercise.currentIndex * 2f` beats for the active chunk. When a chunk
completes before the configured session timer ends, `PracticeViewModel` generates another chunk
in the same key and continues the session.

## Future Matching Modes

Only strict equality is currently implemented. A tolerant mode (`expected subset of played`) can
be introduced by extending `MatchNotesUseCase` and the UI mapper.
