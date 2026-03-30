package com.binbashmedium.sightreadingtrainer.core.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.PedalEvent
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
    private val _pedalEvents = MutableSharedFlow<PedalEvent>(extraBufferCapacity = 32)
    val pedalEvents: SharedFlow<PedalEvent> = _pedalEvents.asSharedFlow()

    private val openRequestTracker = MidiOpenRequestTracker()
    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null
    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (count < 3) return
            val statusByte = msg[offset].toInt() and 0xFF
            val isNoteOn = (statusByte and 0xF0) == 0x90
            val isControlChange = (statusByte and 0xF0) == 0xB0
            val data1 = msg[offset + 1].toInt() and 0xFF
            val velocity = msg[offset + 2].toInt() and 0xFF
            if (isNoteOn && velocity > 0) {
                val midiNote = data1
                scope.launch {
                    _noteEvents.emit(NoteEvent(midiNote, velocity, System.currentTimeMillis()))
                }
            } else if (isControlChange && data1 == 64) {
                val action = if (velocity >= 64) PedalAction.PRESS else PedalAction.RELEASE
                scope.launch {
                    _pedalEvents.emit(PedalEvent(action, System.currentTimeMillis()))
                }
            }
        }
    }

    /** Returns the names of all currently attached MIDI devices. */
    fun getDeviceNames(): List<String> =
        midiManager.devices.map { it.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown" }

    /** Opens the first available MIDI input (from the device's perspective, an output port). */
    fun openDevice(deviceName: String? = null) {
        val deviceInfo = midiManager.devices.firstOrNull { info ->
            deviceName == null ||
                info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) == deviceName
        }
        if (deviceInfo == null) {
            close()
            return
        }

        val requestId = synchronized(this) {
            closeConnectionLocked()
            openRequestTracker.newRequest()
        }

        midiManager.openDevice(deviceInfo, { openedDevice ->
            if (openedDevice != null) {
                synchronized(this) {
                    if (!openRequestTracker.isCurrent(requestId)) {
                        openedDevice.close()
                    } else {
                        closeConnectionLocked()
                        midiDevice = openedDevice
                        val newOutputPort = openedDevice.openOutputPort(0)
                        if (newOutputPort == null) {
                            closeConnectionLocked()
                        } else {
                            outputPort = newOutputPort
                            newOutputPort.connect(midiReceiver)
                        }
                    }
                }
            }
        }, null)
    }

    fun close() {
        synchronized(this) {
            openRequestTracker.invalidate()
            closeConnectionLocked()
        }
    }

    private fun closeConnectionLocked() {
        outputPort?.close()
        outputPort = null
        midiDevice?.close()
        midiDevice = null
    }
}

internal class MidiOpenRequestTracker {
    private var currentRequestId: Int = 0

    fun newRequest(): Int {
        currentRequestId += 1
        return currentRequestId
    }

    fun invalidate() {
        currentRequestId += 1
    }

    fun isCurrent(requestId: Int): Boolean = requestId == currentRequestId
}
