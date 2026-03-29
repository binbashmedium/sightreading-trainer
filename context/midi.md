# MIDI

## Android MIDI API

The app uses `android.media.midi` (API 23+).

Key classes:
- `MidiManager`
- `MidiDeviceInfo`
- `MidiOutputPort`
- `MidiReceiver`

## AndroidMidiManager (`app/core/midi/AndroidMidiManager.kt`)

### Device discovery
```kotlin
fun getDeviceNames(): List<String>
```

### Opening a device
```kotlin
fun openDevice(deviceName: String? = null)
```
Opens matching device (or first available) and listens to output port 0.

### Note parsing
Raw bytes: `[status, note, velocity]`
- `0x9_` + velocity > 0 => NOTE_ON event emitted
- NOTE_ON with velocity 0 is treated as NOTE_OFF and ignored

Emitted through `noteEvents: SharedFlow<domain.NoteEvent>`.

## ChordDetector (`domain/core/midi/ChordDetector.kt`)

Groups near-simultaneous note-on events into one chord event using `chordWindowMs`.

Algorithm:
1. append note to pending buffer
2. restart debounce timer
3. emit accumulated list after silence of `chordWindowMs`

Output: `SharedFlow<List<domain.NoteEvent>>`.

## Staff Split Mapping (UI)

Grand staff rendering uses pitch split at middle C:
- `midi >= 60` => treble staff
- `midi < 60` => bass staff

Implemented in UI helper `midiToGrandStaffY(...)` in `GrandStaffModels.kt`.

## Known Limitations

- Only one MIDI port (index 0) is opened.
- No explicit reconnect flow for disconnect/reconnect events.
- MIDI channel information is not currently used for hand assignment.
