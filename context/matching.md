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
   - Mismatch => `MatchResult.Incorrect`

2. **Timing check (optional)**
   - Runs only when `expectedTimeMs` is provided
   - `delta = playedNotes[0].timestamp - expectedTimeMs`
   - `delta < -toleranceMs` => `TooEarly`
   - `delta > toleranceMs` => `TooLate`
   - otherwise => `Correct`

## MatchResult States

| State | Meaning |
|---|---|
| `Correct` | Notes match, timing within tolerance |
| `Incorrect` | Wrong/missing/extra pitch set |
| `TooEarly` | Correct pitch set but too soon |
| `TooLate` | Correct pitch set but too late |
| `Waiting` | Initial/idle state |

## Practice Session Wrapper

`PracticeSessionUseCase` stores session progress (`Exercise`, `score`, `totalAttempts`) and updates `PracticeState` after each processed chord.

## UI Visualization Mapping

In `PracticeScreen` the domain result is mapped to note states for rendering on the staff:
- `Correct` → expected note glyphs turn green (`NoteState.CORRECT`)
- `Incorrect` → expected glyphs can become red and wrong/extra notes are rendered as red notes on staff (`NoteState.WRONG`)
- `TooLate` → expected note glyphs turn yellow (`NoteState.LATE`)
- pending/unplayed notes remain black (`NoteState.NONE`)

Color is applied directly to musical glyphs (noteheads/stems/flags), not overlays.

## Cursor-time Evaluation Context

The UI also renders a red time cursor based on current beat. Notes left of cursor are interpreted as evaluated/past context; notes right of cursor are upcoming context.

## Future Matching Modes

Only strict equality is currently implemented. A tolerant mode (`expected ⊆ played`) can be introduced by extending `MatchNotesUseCase` and the UI mapper.
