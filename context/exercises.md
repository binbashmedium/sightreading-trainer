# Exercises

## Exercise Model (`domain/model/Exercise.kt`)

```kotlin
data class Exercise(
    val expectedNotes: List<List<Int>>,   // sequence of chords (each chord = list of MIDI notes)
    val currentIndex: Int = 0
)
```

`currentChord` returns the chord at `currentIndex`, or `null` if the exercise is complete.
`isComplete` is true when `currentIndex >= expectedNotes.size`.

## GenerateExerciseUseCase (`domain/usecase/GenerateExerciseUseCase.kt`)

Creates an `Exercise` based on the current `AppSettings` (difficulty + handMode).

### Difficulty Levels

#### Level 1 — Single notes, one hand
C major scale as individual notes. Hand selection from `AppSettings.handMode`:
- RIGHT: C4–C5 (MIDI 60–72)
- LEFT:  C3–B3 (MIDI 48–59)
- BOTH:  right hand scale

#### Level 2 — Both hands, parallel octaves
Right hand (C4–C5) paired with left hand (C3–B3) simultaneously, played together.
Each step is `[leftNote, rightNote]`.

#### Level 3 — Intervals (thirds)
Right-hand diatonic thirds ascending through C major:
```
C+E, D+F, E+G, F+A, G+B, A+C5, B+D5
```

#### Level 4 — Triads in root position
Major and minor triads on each degree of C major:
```
C maj, D min, E min, F maj, G maj, A min, C maj
```
MIDI: `[60,64,67]`, `[62,65,69]`, `[64,67,71]`, `[65,69,72]`, `[67,71,74]`, `[69,72,76]`

#### Level 5 — I–IV–V–I chord progression
```
C major → F major → G major → C major
[60,64,67] → [65,69,72] → [67,71,74] → [60,64,67]
```

### Adding New Exercises

Add a new `private fun generateLevelN(): List<List<Int>>` method and handle it in the `when` expression in `execute()`. No other changes needed.

## PracticeState (`domain/model/PracticeState.kt`)

Snapshot passed to the UI after each chord attempt:

```kotlin
data class PracticeState(
    val exercise: Exercise,
    val lastResult: MatchResult,
    val score: Int,
    val totalAttempts: Int,
    val startTimeMs: Long
)
```

The UI reads:
- `exercise.currentChord` → which notes to display on the staff
- `exercise.isComplete` → show completion screen
- `lastResult` → colour feedback
- `score / totalAttempts` → accuracy display

## Session Lifecycle

```
GenerateExerciseUseCase.execute(settings)
        │
        ▼
  Exercise (immutable)
        │
        ▼  repeated per chord
PracticeSessionUseCase.onChordPlayed(notes)
        │
        ▼
  PracticeState  ──► UI update
        │
  (when isComplete)
        ▼
  Return to MainScreen / restart
```
