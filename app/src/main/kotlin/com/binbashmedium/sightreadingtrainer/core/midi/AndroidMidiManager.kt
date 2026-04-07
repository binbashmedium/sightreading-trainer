// Copyright 2026 BinBashMedium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.binbashmedium.sightreadingtrainer.core.midi

import android.content.Context
import android.os.Build
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.media.midi.MidiDeviceStatus
import com.binbashmedium.sightreadingtrainer.domain.model.NoteEvent
import com.binbashmedium.sightreadingtrainer.domain.model.PedalAction
import com.binbashmedium.sightreadingtrainer.domain.model.PedalEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _availableDeviceNames = MutableStateFlow<List<String>>(emptyList())
    val availableDeviceNames: StateFlow<List<String>> = _availableDeviceNames.asStateFlow()

    private val openRequestTracker = MidiOpenRequestTracker()
    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null
    private var desiredDeviceName: String? = null
    private var connectedDeviceId: Int? = null
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            handleDeviceTopologyChanged()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            handleDeviceTopologyChanged()
        }

        override fun onDeviceStatusChanged(status: MidiDeviceStatus) {
            handleDeviceTopologyChanged()
        }
    }
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

    init {
        refreshAvailableDeviceNames()
        registerDeviceCallback()
    }

    /** Returns the names of all currently attached MIDI devices. */
    fun getDeviceNames(): List<String> =
        availableDeviceNames.value

    /** Opens the first available MIDI input (from the device's perspective, an output port). */
    fun openDevice(deviceName: String? = null) {
        desiredDeviceName = deviceName?.takeIf { it.isNotBlank() }
        val deviceInfo = resolveTargetDeviceInfo()
        if (deviceInfo == null) {
            synchronized(this) {
                closeConnectionLocked()
                openRequestTracker.invalidate()
            }
            refreshAvailableDeviceNames()
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
                        connectedDeviceId = openedDevice.info.id
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


    private fun registerDeviceCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            midiManager.registerDeviceCallback(context.mainExecutor, deviceCallback)
        } else {
            @Suppress("DEPRECATION")
            midiManager.registerDeviceCallback(deviceCallback, null)
        }
    }

    private fun getConnectedDevices(): List<MidiDeviceInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            midiManager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).toList()
        } else {
            @Suppress("DEPRECATION")
            midiManager.devices.toList()
        }

    private fun handleDeviceTopologyChanged() {
        refreshAvailableDeviceNames()
        val shouldReconnect = synchronized(this) {
            val currentId = connectedDeviceId
            if (currentId != null && getConnectedDevices().none { it.id == currentId }) {
                closeConnectionLocked()
            }
            outputPort == null
        }
        if (shouldReconnect) {
            openDevice(desiredDeviceName)
        }
    }

    private fun refreshAvailableDeviceNames() {
        val names = getConnectedDevices()
            .map { it.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown" }
        _availableDeviceNames.value = names
    }

    private fun resolveTargetDeviceInfo(): MidiDeviceInfo? {
        val devices = getConnectedDevices()
        val desired = desiredDeviceName
        return if (desired.isNullOrBlank()) {
            devices.firstOrNull()
        } else {
            devices.firstOrNull { info ->
                info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) == desired
            }
        }
    }

    private fun closeConnectionLocked() {
        outputPort?.close()
        outputPort = null
        midiDevice?.close()
        midiDevice = null
        connectedDeviceId = null
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
