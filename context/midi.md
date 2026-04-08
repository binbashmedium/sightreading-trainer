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

## Chord naming/detection for UI labels

Harmonic naming for rendered labels is implemented in `app/ui/GrandStaffModels.kt` (`detectChord` + `detectChordSuperset`).
The detection now covers a broad set of triad/extended/altered qualities, including:

- triads & suspensions: `M`, `m`, `b5`, `aug`, `dim`, `sus2`, `sus4`, `5`
- sixth/seventh variants: `6`, `m6`, `7`, `M7`, `m7b5`, `dim7`, `7b5`, `M7b5`, `dimM7`, `+7`, `+M7`
- suspended sevenths: `7sus2`, `7sus4`, `M7sus2`, `M7sus4`
- added tones: `add4`, `madd4`, `add9`, `madd9`, `6/9`
- extensions: `9`, `M9`, `mM9`, `11`, `m11`, `M11`, `mM11`, `13`, `m13`, `M13`, `mM13`
- altered dominant forms: `7b9`, `7#9`, `7#11`, `7b13`

Detection is pitch-class-set based (root + quality from modulo-12 classes), so enharmonic/add-tone aliases that share the same set (e.g. `add2`/`add9`) use one canonical label in output.

When a detected chord is in inversion (lowest played pitch class differs from detected root),
the rendered chord label now uses slash-bass notation, e.g. `CM/E`, `G7/F`, `Dm/A`.

## Staff Split Mapping (UI)

Grand staff rendering uses pitch split at middle C:
- `midi >= 60` => treble staff
- `midi < 60` => bass staff

Implemented in UI helper `midiToGrandStaffY(...)` in `GrandStaffModels.kt`.

## Known Limitations

- Only one MIDI port (index 0) is opened.
- MIDI channel information is not currently used for hand assignment.
