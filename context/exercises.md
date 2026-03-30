# Exercises

## Exercise Model (`domain/model/Exercise.kt`)

```kotlin
data class Exercise(
    val expectedNotes: List<List<Int>>,   // sequence of chords (each chord = list of MIDI notes)
    val currentIndex: Int = 0,
    val musicalKey: Int = 0               // 0=C … 11=B, set at generation time
)
```

`currentChord` returns the chord at `currentIndex`, or `null` if complete.
`isComplete` is true when `currentIndex >= expectedNotes.size`.

## GenerateExerciseUseCase (`domain/usecase/GenerateExerciseUseCase.kt`)

Creates an `Exercise` from current `AppSettings` (difficulty + hand mode + musicalKey).

Notes are transposed by `musicalKey` semitones from C. All note lists are **shuffled** using
`kotlin.random.Random` so every call produces a different note order.

### Difficulty levels

| Level | Content | Notes per chord | Count |
|---|---|---|---|
| 1 | Single notes from diatonic scale (one hand, shuffled) | 1 | 8 |
| 2 | Parallel octaves (both hands, shuffled) | 2 | 7 |
| 3 | Diatonic thirds (right hand, shuffled) | 2 | 7 |
| 4 | All 7 diatonic triads root position (shuffled) | 3 | 7 |
| 5 | Randomly selected 4-chord progression (I–IV–V–I, I–V–vi–IV, I–vi–IV–V, I–ii–V–I) | 3 | 4 |

### Key constants

```kotlin
GenerateExerciseUseCase.KEY_NAMES  // ["C","C#","D","Eb","E","F","F#","G","Ab","A","Bb","B"]
```

Also exposed from `app/ui/GrandStaffModels.kt` as top-level `KEY_NAMES` for use in UI.

## Practice UI Data Model (app layer)

Practice rendering is driven by a dedicated UI model in
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
  val bpm: Float,          // live BPM from last inter-chord interval
  val notes: List<NoteEvent>,
  val chords: List<Chord>,
  val currentBeat: Float   // static: exercise.currentIndex * 2f (not time-driven)
)
```

## Mapping from Domain to UI State

`PracticeScreen.toGameState(nowMs)` maps `PracticeState` to `GameState`:
- `levelTitle` = `"$keyName · $levelDesc"` (e.g. "G · Triads / Progression")
- `elapsedTime` = wall-clock elapsed since session start
- `score` and `bpm` taken directly from `PracticeState`
- Notes coloured per-beat using `PracticeState.resultByBeat[beatIndex]`
- `currentBeat` = `exercise.currentIndex * 2f` (cursor is **static**, not time-driven)

## Session Lifecycle

```
GenerateExerciseUseCase.execute(settings)  ← random each call
        │
        ▼
  Exercise (immutable snapshot)
        │
        ▼  for each detected MIDI chord
PracticeSessionUseCase.processChord(notes)
        │
        ▼
  PracticeState  ──► toGameState(nowMs) ──► GrandStaffCanvas
```

## Extending Exercises

To add a new exercise set:
1. Add generation logic in `GenerateExerciseUseCase` (add to `execute` when block).
2. Ensure output is `List<List<Int>>` MIDI groups.
3. UI picks up new content automatically through `PracticeState → GameState` mapping.
