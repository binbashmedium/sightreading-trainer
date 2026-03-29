package com.binbashmedium.sightreadingtrainer.core.midi

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Android [MidiManager] API, opens the first available MIDI device
 * and exposes incoming note-on events as a [SharedFlow].
 */
@Singleton
class AndroidMidiManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _noteEvents = MutableSharedFlow<NoteEvent>(extraBufferCapacity = 64)
    val noteEvents: SharedFlow<NoteEvent> = _noteEvents.asSharedFlow()

    private var outputPort: MidiOutputPort? = null

    /** Returns the names of all currently attached MIDI devices. */
    fun getDeviceNames(): List<String> =
        midiManager.devices.map { it.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown" }

    /** Opens the first available MIDI input (from the device's perspective, an output port). */
    fun openDevice(deviceName: String? = null) {
        val device = midiManager.devices.firstOrNull { info ->
            deviceName == null ||
                info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) == deviceName
        } ?: return

        midiManager.openDevice(device, { openedDevice ->
            openedDevice ?: return@openDevice
            outputPort = openedDevice.openOutputPort(0)
            outputPort?.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    if (count < 3) return
                    val statusByte = msg[offset].toInt() and 0xFF
                    val isNoteOn = (statusByte and 0xF0) == 0x90
                    val velocity = msg[offset + 2].toInt() and 0xFF
                    if (isNoteOn && velocity > 0) {
                        val midiNote = msg[offset + 1].toInt() and 0xFF
                        scope.launch {
                            _noteEvents.emit(NoteEvent(midiNote, velocity, System.currentTimeMillis()))
                        }
                    }
                }
            })
        }, null)
    }

    fun close() {
        outputPort?.close()
        outputPort = null
    }
}
