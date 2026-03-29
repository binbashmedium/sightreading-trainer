# Note Matching

## MatchNotesUseCase (`domain/usecase/MatchNotesUseCase.kt`)

Stateless use-case. Called once per chord event to evaluate whether the player hit the right notes at the right time.

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

**Step 1 — Pitch matching (strict)**
Extracts MIDI note numbers from `playedNotes`, sorts both lists, and compares:
```
played.sorted() == expected.sorted()  →  pitch match
```
If they differ → `MatchResult.Incorrect` (no timing check performed).

**Step 2 — Timing check (optional)**
Only evaluated when `expectedTimeMs` is provided (beat-synchronised exercises).
```
delta = playedNotes[0].timestamp - expectedTimeMs

delta < -toleranceMs  →  TooEarly
delta >  toleranceMs  →  TooLate
|delta| <= toleranceMs →  Correct
```

**Default tolerance:** 200 ms (configurable via `AppSettings.timingToleranceMs`).

### MatchResult States

| State | Meaning |
|---|---|
| `Correct` | All notes match, timing within tolerance |
| `Incorrect` | Wrong notes played (missing, extra, or wrong pitch) |
| `TooEarly` | Correct notes but played too soon |
| `TooLate` | Correct notes but played too late |
| `Waiting` | Initial/idle state before any attempt |

## PracticeSessionUseCase (`domain/usecase/PracticeSessionUseCase.kt`)

Stateful wrapper around `MatchNotesUseCase`. Owns an `Exercise` and advances through it.

### Key Method
```kotlin
fun onChordPlayed(playedNotes: List<NoteEvent>): PracticeState
```
1. Gets `exercise.currentChord` (the expected notes for this step)
2. Calls `MatchNotesUseCase.execute(...)`
3. On `Correct`: increments `exercise.currentIndex` and `score`
4. Always increments `totalAttempts`
5. Returns updated `PracticeState`

### Score Calculation
```
score / totalAttempts × 100  →  accuracy percentage
```

## Visual Feedback

`PracticeScreen` maps `MatchResult` to colour:
- `Correct` → green
- `Incorrect`, `TooEarly`, `TooLate` → red
- `Waiting` → neutral

The feedback is shown below the staff and resets when the next chord is expected.

## Matching Modes (Future)

Currently only **strict** matching is implemented (exact note set equality). The architecture supports adding a **tolerant** mode (expected notes ⊆ played notes) by extending `MatchNotesUseCase` with a `mode` parameter.
