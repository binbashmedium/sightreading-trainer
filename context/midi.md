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

Connection lifecycle is now guarded against stale async callbacks:
- existing output port/device are closed before each reopen
- each open request gets a monotonically increasing request ID
- callback attachment is applied only if request ID is still current
- `close()` invalidates pending opens and closes both the port and device

This prevents duplicate receiver attachments after settings-driven reopens (for example after a completed session updates highscore/counters).

### Reconnect behavior

`AndroidMidiManager` now registers a `MidiManager.DeviceCallback` and keeps a desired device name.
When device topology changes:
- available MIDI device names are refreshed reactively
- disconnected active devices are closed
- if no active port is open, the manager attempts to reconnect to the desired device (or first available device)

This prevents silent MIDI failure after unplug/replug.

### Note parsing
Raw bytes: `[status, note, velocity]`
- `0x9_` + velocity > 0 => NOTE_ON event emitted
- NOTE_ON with velocity 0 is treated as NOTE_OFF and ignored

Emitted through `noteEvents: SharedFlow<domain.NoteEvent>`.

### Pedal support
Sustain pedal is normally MIDI Control Change 64 (`0xB_`, controller `64`, value >= 64 down / < 64 up).
The app now parses CC64 pedal press/release messages and surfaces them through `AndroidMidiManager`, then groups them with nearby note-on events in `ChordDetector` so pedal marks can be validated on the same beat as notes.

## ChordDetector (`domain/core/midi/ChordDetector.kt`)

Groups near-simultaneous note-on and pedal events into one performance input using `chordWindowMs`.

Algorithm:
1. append note to pending buffer
2. restart debounce timer
3. emit accumulated list after silence of `chordWindowMs`

Output: `SharedFlow<domain.PerformanceInput>`.

## Staff Split Mapping (UI)

Grand staff rendering uses pitch split at middle C:
- `midi >= 60` => treble staff
- `midi < 60` => bass staff

Implemented in UI helper `midiToGrandStaffY(...)` in `GrandStaffModels.kt`.

## Known Limitations

- Only one MIDI port (index 0) is opened.
- No explicit reconnect flow for disconnect/reconnect events.
- MIDI channel information is not currently used for hand assignment.
