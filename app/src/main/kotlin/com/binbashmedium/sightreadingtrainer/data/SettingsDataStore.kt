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

package com.binbashmedium.sightreadingtrainer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import com.binbashmedium.sightreadingtrainer.domain.model.ChordProgression
import com.binbashmedium.sightreadingtrainer.domain.model.ExerciseContentType
import com.binbashmedium.sightreadingtrainer.domain.model.HandMode
import com.binbashmedium.sightreadingtrainer.domain.model.NoteValue
import com.binbashmedium.sightreadingtrainer.domain.model.OrnamentType
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
        val EXERCISE_TIME_MIN = intPreferencesKey("exercise_time_min")
        val EXERCISE_TYPES = stringPreferencesKey("exercise_types")
        val HAND_MODE = stringPreferencesKey("hand_mode")
        val NOTE_ACCIDENTALS_ENABLED = booleanPreferencesKey("note_accidentals_enabled")
        val PEDAL_EVENTS_ENABLED = booleanPreferencesKey("pedal_events_enabled")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val TOTAL_CORRECT_NOTES = intPreferencesKey("total_correct_notes")
        val TOTAL_WRONG_NOTES = intPreferencesKey("total_wrong_notes")
        val CORRECT_GROUP_STATS = stringPreferencesKey("correct_group_stats")
        val WRONG_GROUP_STATS = stringPreferencesKey("wrong_group_stats")
        val CORRECT_NOTE_STATS = stringPreferencesKey("correct_note_stats")
        val WRONG_NOTE_STATS = stringPreferencesKey("wrong_note_stats")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SELECTED_KEYS = stringPreferencesKey("selected_keys")
        val MUSICAL_KEY = intPreferencesKey("musical_key")
        val SELECTED_PROGRESSIONS = stringPreferencesKey("selected_progressions")
        val SELECTED_NOTE_VALUES = stringPreferencesKey("selected_note_values")
        val CHORD_NAMES_ENABLED = booleanPreferencesKey("chord_names_enabled")
        val BASS_NOTE_RANGE_MIN = intPreferencesKey("bass_note_range_min")
        val BASS_NOTE_RANGE_MAX = intPreferencesKey("bass_note_range_max")
        val TREBLE_NOTE_RANGE_MIN = intPreferencesKey("treble_note_range_min")
        val TREBLE_NOTE_RANGE_MAX = intPreferencesKey("treble_note_range_max")
        val SELECTED_ORNAMENTS = stringPreferencesKey("selected_ornaments")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            midiDeviceName = prefs[Keys.MIDI_DEVICE] ?: "",
            timingToleranceMs = prefs[Keys.TIMING_TOLERANCE] ?: 200,
            chordWindowMs = prefs[Keys.CHORD_WINDOW] ?: 50,
            exerciseTimeMin = (prefs[Keys.EXERCISE_TIME_MIN] ?: 1).coerceIn(1, 10),
            exerciseTypes = parseExerciseTypes(
                prefs[Keys.EXERCISE_TYPES],
                prefs[intPreferencesKey("difficulty")] ?: 1
            ),
            handMode = HandMode.valueOf(prefs[Keys.HAND_MODE] ?: HandMode.RIGHT.name),
            noteAccidentalsEnabled = prefs[Keys.NOTE_ACCIDENTALS_ENABLED] ?: false,
            pedalEventsEnabled = prefs[Keys.PEDAL_EVENTS_ENABLED] ?: false,
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            totalCorrectNotes = prefs[Keys.TOTAL_CORRECT_NOTES] ?: 0,
            totalWrongNotes = prefs[Keys.TOTAL_WRONG_NOTES] ?: 0,
            correctGroupStats = parseStatsMap(prefs[Keys.CORRECT_GROUP_STATS]),
            wrongGroupStats = parseStatsMap(prefs[Keys.WRONG_GROUP_STATS]),
            correctNoteStats = parseStatsMap(prefs[Keys.CORRECT_NOTE_STATS]),
            wrongNoteStats = parseStatsMap(prefs[Keys.WRONG_NOTE_STATS]),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            selectedKeys = parseSelectedKeys(
                prefs[Keys.SELECTED_KEYS],
                prefs[Keys.MUSICAL_KEY] ?: 0
            ),
            selectedProgressions = parseProgressions(prefs[Keys.SELECTED_PROGRESSIONS]),
            selectedNoteValues = parseNoteValues(prefs[Keys.SELECTED_NOTE_VALUES]),
            chordNamesEnabled = prefs[Keys.CHORD_NAMES_ENABLED] ?: false,
            bassNoteRangeMin = (prefs[Keys.BASS_NOTE_RANGE_MIN] ?: 28).coerceIn(28, 72),
            bassNoteRangeMax = (prefs[Keys.BASS_NOTE_RANGE_MAX] ?: 60).coerceIn(28, 72),
            trebleNoteRangeMin = (prefs[Keys.TREBLE_NOTE_RANGE_MIN] ?: 60).coerceIn(48, 93),
            trebleNoteRangeMax = (prefs[Keys.TREBLE_NOTE_RANGE_MAX] ?: 84).coerceIn(48, 93),
            selectedOrnaments = parseSelectedOrnaments(prefs[Keys.SELECTED_ORNAMENTS])
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MIDI_DEVICE] = settings.midiDeviceName
            prefs[Keys.TIMING_TOLERANCE] = settings.timingToleranceMs
            prefs[Keys.CHORD_WINDOW] = settings.chordWindowMs
            prefs[Keys.EXERCISE_TIME_MIN] = settings.exerciseTimeMin
            prefs[Keys.EXERCISE_TYPES] = settings.exerciseTypes.sortedBy { it.ordinal }.joinToString(",") { it.name }
            prefs[Keys.HAND_MODE] = settings.handMode.name
            prefs[Keys.NOTE_ACCIDENTALS_ENABLED] = settings.noteAccidentalsEnabled
            prefs[Keys.PEDAL_EVENTS_ENABLED] = settings.pedalEventsEnabled
            prefs[Keys.HIGH_SCORE] = settings.highScore
            prefs[Keys.TOTAL_CORRECT_NOTES] = settings.totalCorrectNotes
            prefs[Keys.TOTAL_WRONG_NOTES] = settings.totalWrongNotes
            prefs[Keys.CORRECT_GROUP_STATS] = serializeStatsMap(settings.correctGroupStats)
            prefs[Keys.WRONG_GROUP_STATS] = serializeStatsMap(settings.wrongGroupStats)
            prefs[Keys.CORRECT_NOTE_STATS] = serializeStatsMap(settings.correctNoteStats)
            prefs[Keys.WRONG_NOTE_STATS] = serializeStatsMap(settings.wrongNoteStats)
            prefs[Keys.SOUND_ENABLED] = settings.soundEnabled
            prefs[Keys.SELECTED_KEYS] = settings.selectedKeys.sorted().joinToString(",")
            prefs[Keys.MUSICAL_KEY] = settings.selectedKeys.minOrNull() ?: 0
            prefs[Keys.SELECTED_PROGRESSIONS] = serializeProgressions(settings.selectedProgressions)
            prefs[Keys.SELECTED_NOTE_VALUES] = settings.selectedNoteValues
                .sortedBy { it.ordinal }
                .joinToString(",") { it.name }
            prefs[Keys.CHORD_NAMES_ENABLED] = settings.chordNamesEnabled
            prefs[Keys.BASS_NOTE_RANGE_MIN] = settings.bassNoteRangeMin
            prefs[Keys.BASS_NOTE_RANGE_MAX] = settings.bassNoteRangeMax
            prefs[Keys.TREBLE_NOTE_RANGE_MIN] = settings.trebleNoteRangeMin
            prefs[Keys.TREBLE_NOTE_RANGE_MAX] = settings.trebleNoteRangeMax
            prefs[Keys.SELECTED_ORNAMENTS] = settings.selectedOrnaments
                .sortedBy { it.ordinal }.joinToString(",") { it.name }
        }
    }

    private fun parseSelectedKeys(raw: String?, fallbackKey: Int): Set<Int> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 0..11 }
            ?.toSet()
            .orEmpty()

        return if (parsed.isNotEmpty()) parsed else setOf(fallbackKey.coerceIn(0, 11))
    }

    private fun parseExerciseTypes(
        raw: String?,
        legacyDifficulty: Int
    ): Set<ExerciseContentType> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { name -> ExerciseContentType.entries.find { it.name == name.trim() } }
            ?.toSet()
            .orEmpty()

        if (parsed.isNotEmpty()) return parsed

        return when (legacyDifficulty) {
            1 -> setOf(ExerciseContentType.SINGLE_NOTES)
            2 -> setOf(ExerciseContentType.OCTAVES)
            3 -> setOf(ExerciseContentType.THIRDS)
            4 -> setOf(ExerciseContentType.TRIADS, ExerciseContentType.ARPEGGIOS)
            5 -> setOf(ExerciseContentType.TRIADS, ExerciseContentType.SEVENTHS, ExerciseContentType.NINTHS, ExerciseContentType.ARPEGGIOS)
            else -> setOf(ExerciseContentType.SINGLE_NOTES)
        }
    }

    private fun parseStatsMap(raw: String?): Map<String, Int> =
        raw
            ?.split(";")
            ?.mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = entry.substring(0, idx)
                val value = entry.substring(idx + 1).toIntOrNull() ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()

    private fun serializeStatsMap(map: Map<String, Int>): String =
        map.entries
            .filter { it.key.isNotBlank() && it.value > 0 }
            .sortedBy { it.key }
            .joinToString(";") { "${it.key}=${it.value}" }

    private fun parseProgressions(raw: String?): Set<ChordProgression> = parseSelectedProgressions(raw)

    private fun parseNoteValues(raw: String?): Set<NoteValue> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { name -> NoteValue.entries.find { it.name == name.trim() } }
            ?.toSet()
            .orEmpty()
        return if (parsed.isNotEmpty()) parsed else NoteValue.entries.toSet()
    }
}

internal fun serializeProgressions(progressions: Set<ChordProgression>): String =
    progressions
        .sortedBy { it.ordinal }
        .joinToString(",") { it.name }

internal fun parseSelectedProgressions(raw: String?): Set<ChordProgression> {
    val parsed = raw
        ?.split(",")
        ?.mapNotNull { name -> ChordProgression.entries.find { it.name == name.trim() } }
        ?.toSet()
        .orEmpty()

    return if (parsed.isNotEmpty()) parsed else setOf(ChordProgression.I_IV_V_I)
}

internal fun parseSelectedOrnaments(raw: String?): Set<OrnamentType> =
    raw
        ?.split(",")
        ?.mapNotNull { name -> OrnamentType.entries.find { it.name == name.trim() && it != OrnamentType.NONE } }
        ?.toSet()
        .orEmpty()
