# Exercises

## Exercise Model (`domain/model/Exercise.kt`)

```kotlin
data class Exercise(
    val expectedNotes: List<List<Int>>,   // sequence of chords (each chord = list of MIDI notes)
    val currentIndex: Int = 0
)
```

`currentChord` returns the chord at `currentIndex`, or `null` if complete.
`isComplete` is true when `currentIndex >= expectedNotes.size`.

## GenerateExerciseUseCase (`domain/usecase/GenerateExerciseUseCase.kt`)

Creates an `Exercise` from current `AppSettings` (difficulty + hand mode).

### Difficulty levels

1. Single notes (one hand)
2. Parallel octaves (both hands)
3. Intervals (thirds)
4. Root-position triads
5. I–IV–V–I progression

(Exact per-level content remains in `GenerateExerciseUseCase.kt`.)

## Practice UI Data Model (app layer)

Practice rendering is now driven by a dedicated UI model in
`app/src/main/kotlin/com/binbashmedium/sightreadingtrainer/ui/GrandStaffModels.kt`:

```kotlin
enum class NoteState { NONE, CORRECT, WRONG, LATE }

data class NoteEvent(
  val midi: Int,
  val startBeat: Float,
  val duration: Float,
  val expected: Boolean,
  val state: NoteState = NoteState.NONE
)

data class Chord(val name: String, val notes: List<Int>, val startBeat: Float)

data class GameState(
  val levelTitle: String,
  val elapsedTime: Long,
  val score: Int,
  val notes: List<NoteEvent>,
  val chords: List<Chord>,
  val currentBeat: Float
)
```

## Mapping from Domain to UI State

`PracticeScreen` maps `PracticeState` to `GameState` with:
- dynamic level title
- elapsed time from `startTimeMs`
- dynamic score
- expected notes mapped to beats and durations
- dynamic chord labels derived from generated exercise notes
- optional extra wrong notes rendered in red when incorrect input is detected

This keeps the staff content state-driven without hardcoding fixed chord/time strings in UI.

## Session Lifecycle

```
GenerateExerciseUseCase.execute(settings)
        │
        ▼
  Exercise (immutable snapshot)
        │
        ▼  repeated per detected chord
PracticeSessionUseCase.processChord(notes)
        │
        ▼
  PracticeState  ──► PracticeScreen.toGameState(...) ──► GrandStaffCanvas
```

## Extending Exercises

To add a new exercise set:
1. Add generation logic in `GenerateExerciseUseCase`.
2. Ensure output remains `List<List<Int>>` MIDI groups.
3. UI will pick up new content automatically through `PracticeState -> GameState` mapping.
