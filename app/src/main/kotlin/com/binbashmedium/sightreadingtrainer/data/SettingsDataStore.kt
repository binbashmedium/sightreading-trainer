package com.binbashmedium.sightreadingtrainer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MIDI_DEVICE = stringPreferencesKey("midi_device")
        val TIMING_TOLERANCE = intPreferencesKey("timing_tolerance_ms")
        val CHORD_WINDOW = intPreferencesKey("chord_window_ms")
        val DIFFICULTY = intPreferencesKey("difficulty")
        val HAND_MODE = stringPreferencesKey("hand_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            midiDeviceName = prefs[Keys.MIDI_DEVICE] ?: "",
            timingToleranceMs = prefs[Keys.TIMING_TOLERANCE] ?: 200,
            chordWindowMs = prefs[Keys.CHORD_WINDOW] ?: 50,
            difficulty = prefs[Keys.DIFFICULTY] ?: 1,
            handMode = HandMode.valueOf(prefs[Keys.HAND_MODE] ?: HandMode.RIGHT.name),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MIDI_DEVICE] = settings.midiDeviceName
            prefs[Keys.TIMING_TOLERANCE] = settings.timingToleranceMs
            prefs[Keys.CHORD_WINDOW] = settings.chordWindowMs
            prefs[Keys.DIFFICULTY] = settings.difficulty
            prefs[Keys.HAND_MODE] = settings.handMode.name
            prefs[Keys.SOUND_ENABLED] = settings.soundEnabled
        }
    }
}
