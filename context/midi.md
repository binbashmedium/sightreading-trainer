# MIDI

## Android MIDI API

The app uses `android.media.midi` (API 23+). No third-party MIDI library is needed.

**Key classes used:**
- `MidiManager` — system service, lists devices, opens connections
- `MidiDeviceInfo` — metadata for a connected device (name, port count)
- `MidiOutputPort` — the port the keyboard sends notes FROM (confusingly named from the device's perspective)
- `MidiReceiver` — callback receiving raw MIDI byte arrays

## AndroidMidiManager (`app/core/midi/AndroidMidiManager.kt`)

Singleton injected with `@ApplicationContext`.

### Device discovery
```kotlin
fun getDeviceNames(): List<String>
```
Returns names of all currently attached MIDI devices. Used in Settings to populate the device selector.

### Opening a device
```kotlin
fun openDevice(deviceName: String? = null)
```
Opens the first device that matches `deviceName` (or the first device overall if `null`).
Connects a `MidiReceiver` to output port 0.

### Note parsing
Raw MIDI bytes follow the format `[status, note, velocity]`.
- Status byte high nibble `0x9_` = NOTE_ON
- NOTE_ON with velocity 0 is treated as NOTE_OFF (standard MIDI convention) and ignored
- Valid NOTE_ON events are emitted on `noteEvents: SharedFlow<NoteEvent>`

```
Byte layout:  [status | channel]  [midiNote 0–127]  [velocity 0–127]
              0x9n                n                  v
```

### Limitations / known issues
- Only port 0 is opened; devices with multiple ports are not fully supported
- `MidiManager.getDevices()` is deprecated in API 33 — a future update should use `getMidiDevices()` with callback
- No reconnect logic on device disconnect

## ChordDetector (`domain/core/midi/ChordDetector.kt`)

Pure Kotlin, no Android dependency.

### Purpose
A piano player pressing a chord never hits all keys at exactly the same millisecond. The ChordDetector treats all notes arriving within `chordWindowMs` (default 50 ms) as a single chord.

### Algorithm
1. On `onNoteOn(midiNote, velocity, timestamp)`:
   - Add event to `pendingNotes`
   - Cancel the previous timer job
   - Start a new coroutine that waits `chordWindowMs` then emits `pendingNotes` as a chord
2. Notes within the window keep resetting the timer
3. After `chordWindowMs` of silence, the accumulated notes are emitted on `chords: SharedFlow<List<NoteEvent>>`

### Configuration
`chordWindowMs` is read from `AppSettings.chordWindowMs` (user-adjustable, default 50 ms).

## Hand Assignment

Left/right hand distinction is based purely on MIDI note number:
- `midiNote < 60` (below middle C) → LEFT hand
- `midiNote >= 60` → RIGHT hand

This default covers a standard split for a beginner pianist. There is no per-channel assignment — consumer USB keyboards typically use a single MIDI channel regardless of hand position.
