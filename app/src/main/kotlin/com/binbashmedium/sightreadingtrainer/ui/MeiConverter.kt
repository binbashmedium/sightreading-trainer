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

package com.binbashmedium.sightreadingtrainer.ui

import com.binbashmedium.sightreadingtrainer.domain.model.NoteAccidental
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Converts a [GameState] to MEI XML suitable for rendering with Verovio.
 *
 * Beat unit conventions:
 * - [NoteEvent.startBeat] is in UI beat-units (2 per quarter note, BEATS_PER_STEP = 2f).
 * - [NoteEvent.duration]  is in quarter-note beats (WHOLE=4, HALF=2, QUARTER=1, EIGHTH=0.5).
 * - One measure = BEATS_PER_MEASURE_UNITS (8) UI beat-units = 4 quarter-note beats.
 *
 * MEI note IDs:
 * - Regular note: "nb{beatKey}m{midi}" where beatKey = (startBeat * 10).roundToLong()
 * - Current-step note: "ncurr{beatKey}m{midi}" — picked up by JS drawCursor()
 */
object MeiConverter {

    /**
     * Build the full MEI XML document for the given beat range.
     *
     * @param gameState  Source game state containing notes, key, etc.
     * @param startBeat  First UI beat-unit to include (inclusive).
     * @param endBeat    Last UI beat-unit to exclude (pass [Float.MAX_VALUE] to include all).
     */
    fun convert(gameState: GameState, startBeat: Float, endBeat: Float): String {
        val actualEnd = if (endBeat >= Float.MAX_VALUE / 2f) {
            (gameState.notes.maxOfOrNull { it.startBeat } ?: startBeat) + BEATS_PER_MEASURE_UNITS
        } else endBeat

        val notes = gameState.notes.filter {
            it.startBeat >= startBeat && it.startBeat < actualEnd
        }

        val firstMeasure = (startBeat / BEATS_PER_MEASURE_UNITS).toInt()
        val lastMeasure  = ((actualEnd - 0.01f) / BEATS_PER_MEASURE_UNITS).toInt()
        val numMeasures  = lastMeasure - firstMeasure + 1

        val keySigAttr = keySignatureAttr(gameState.musicalKey)

        val measuresXml = buildString {
            for (i in 0 until numMeasures) {
                val mIdx          = firstMeasure + i
                val mStartBeat    = mIdx * BEATS_PER_MEASURE_UNITS
                val mEndBeat      = mStartBeat + BEATS_PER_MEASURE_UNITS
                val measureNotes  = notes.filter { it.startBeat >= mStartBeat && it.startBeat < mEndBeat }
                val isLast        = i == numMeasures - 1
                append(renderMeasure(i + 1, measureNotes, mStartBeat, isLast, gameState.currentBeat))
            }
        }

        return buildMeiDocument(keySigAttr, measuresXml)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildMeiDocument(keySigAttr: String, measuresXml: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<mei xmlns="http://www.music-encoding.org/ns/mei" meiversion="4.0.0">
  <meiHead><fileDesc><titleStmt><title/></titleStmt><pubStmt/></fileDesc></meiHead>
  <music><body><mdiv><score>
    <scoreDef meter.count="4" meter.unit="4" $keySigAttr>
      <staffGrp symbol="brace" barthru="true">
        <staffDef n="1" lines="5" clef.shape="G" clef.line="2"/>
        <staffDef n="2" lines="5" clef.shape="F" clef.line="4"/>
      </staffGrp>
    </scoreDef>
    <section>
$measuresXml    </section>
  </score></mdiv></body></music>
</mei>"""

    private fun renderMeasure(
        n: Int,
        notes: List<NoteEvent>,
        measureStartBeat: Float,
        isLast: Boolean,
        currentBeat: Float
    ): String {
        val treble = notes.filter { it.staff == StaffType.TREBLE }
        val bass   = notes.filter { it.staff == StaffType.BASS }
        val right  = if (isLast) """ right="end"""" else ""
        return "      <measure n=\"$n\"$right>\n" +
               "        <staff n=\"1\"><layer n=\"1\">\n" +
               renderLayer(treble, measureStartBeat, currentBeat).prependIndent("          ") + "\n" +
               "        </layer></staff>\n" +
               "        <staff n=\"2\"><layer n=\"1\">\n" +
               renderLayer(bass, measureStartBeat, currentBeat).prependIndent("          ") + "\n" +
               "        </layer></staff>\n" +
               "      </measure>\n"
    }

    /**
     * Generates MEI layer content for one staff within one measure.
     * Fills gaps between notes with rests to complete the 4/4 measure.
     */
    internal fun renderLayer(
        notes: List<NoteEvent>,
        measureStartBeat: Float,
        currentBeat: Float
    ): String {
        val byBeat = notes.groupBy { it.startBeat }.toSortedMap()
        if (byBeat.isEmpty()) return "<mRest/>"

        val sb = StringBuilder()
        var currentQBeat = 0f  // quarter-note beats from measure start

        byBeat.forEach { (startBeat, chordNotes) ->
            val qBeat     = (startBeat - measureStartBeat) / BEATS_PER_STEP
            val duration  = chordNotes.first().duration   // quarter-note beats
            val isCurrent = abs(startBeat - currentBeat) < 0.01f

            // Fill gap before this chord with a rest
            val gap = qBeat - currentQBeat
            if (gap > 0.01f) sb.append(rest(gap)).append("\n")

            sb.append(renderChord(chordNotes, duration, startBeat, isCurrent)).append("\n")
            currentQBeat = qBeat + duration
        }

        // Fill tail of measure with a rest
        val tail = 4f - currentQBeat
        if (tail > 0.01f) sb.append(rest(tail)).append("\n")

        return sb.toString().trimEnd()
    }

    private fun renderChord(
        notes: List<NoteEvent>,
        duration: Float,
        startBeat: Float,
        isCurrent: Boolean
    ): String {
        val dur     = quarterBeatsToDur(duration)
        val beatKey = (startBeat * 10).roundToLong()
        val prefix  = if (isCurrent) "ncurr" else "nb"

        return if (notes.size == 1) {
            noteElement(notes.first(), dur, "${prefix}${beatKey}m${notes.first().midi}")
        } else {
            val id = "chord-${prefix}${beatKey}"
            val noteLines = notes.joinToString("\n") { note ->
                noteElement(note, dur, "${prefix}${beatKey}m${note.midi}")
            }
            "<chord dur=\"$dur\" xml:id=\"$id\">\n$noteLines\n</chord>"
        }
    }

    private fun noteElement(note: NoteEvent, dur: String, id: String): String {
        val (pname, oct, accidGes) = midiToMeiPitch(note.midi, note.accidental)
        val color      = noteStateMeiColor(note.state)
        val colorAttr  = if (color != null) " color=\"$color\"" else ""
        val accGesAttr = if (accidGes != null) " accid.ges=\"$accidGes\"" else ""
        val accAttr    = visualAccidAttr(note.accidental, accidGes)
        return "<note pname=\"$pname\" oct=\"$oct\" dur=\"$dur\"" +
               " xml:id=\"$id\"$accGesAttr$accAttr$colorAttr/>"
    }

    private fun rest(quarterBeats: Float): String = "<rest dur=\"${quarterBeatsToDur(quarterBeats)}\"/>"

    // ── Pitch conversion ──────────────────────────────────────────────────────

    /**
     * Converts a MIDI note number to MEI pitch components.
     *
     * @return Triple(pname, oct, accid.ges) where accid.ges is null (natural), "s" (sharp), or "f" (flat).
     */
    internal fun midiToMeiPitch(midi: Int, accidental: NoteAccidental): Triple<String, Int, String?> {
        val pitchClass = ((midi % 12) + 12) % 12
        val oct = (midi / 12) - 1
        return when (pitchClass) {
            0  -> Triple("c", oct, null)
            1  -> if (accidental == NoteAccidental.FLAT) Triple("d", oct, "f") else Triple("c", oct, "s")
            2  -> Triple("d", oct, null)
            3  -> if (accidental == NoteAccidental.FLAT) Triple("e", oct, "f") else Triple("d", oct, "s")
            4  -> Triple("e", oct, null)
            5  -> Triple("f", oct, null)
            6  -> if (accidental == NoteAccidental.FLAT) Triple("g", oct, "f") else Triple("f", oct, "s")
            7  -> Triple("g", oct, null)
            8  -> if (accidental == NoteAccidental.FLAT) Triple("a", oct, "f") else Triple("g", oct, "s")
            9  -> Triple("a", oct, null)
            10 -> if (accidental == NoteAccidental.FLAT) Triple("b", oct, "f") else Triple("a", oct, "s")
            11 -> Triple("b", oct, null)
            else -> Triple("c", oct, null)
        }
    }

    /** Returns the visual `accid` attribute string for a note, or empty string if none needed. */
    private fun visualAccidAttr(accidental: NoteAccidental, accidGes: String?): String = when (accidental) {
        NoteAccidental.NATURAL -> " accid=\"n\""
        NoteAccidental.SHARP   -> if (accidGes == "s") " accid=\"s\"" else ""
        NoteAccidental.FLAT    -> if (accidGes == "f") " accid=\"f\"" else ""
        NoteAccidental.NONE    -> ""
    }

    // ── Duration conversion ───────────────────────────────────────────────────

    /**
     * Converts quarter-note beat count to MEI `dur` attribute value.
     * Whole=1, Half=2, Quarter=4, Eighth=8, Sixteenth=16.
     */
    internal fun quarterBeatsToDur(quarterBeats: Float): String = when {
        quarterBeats >= 4f   -> "1"
        quarterBeats >= 2f   -> "2"
        quarterBeats >= 1f   -> "4"
        quarterBeats >= 0.5f -> "8"
        else                 -> "16"
    }

    // ── Key signature ─────────────────────────────────────────────────────────

    /**
     * Returns the MEI `key.sig` attribute string for the given musical key (0–11).
     * E.g. "1s" for G major (1 sharp), "2f" for Bb major (2 flats), "0" for C major.
     */
    internal fun keySignatureAttr(musicalKey: Int): String {
        val (sharps, flats) = KEY_SIGNATURES.getOrElse(musicalKey) { 0 to 0 }
        return when {
            sharps > 0 -> "key.sig=\"${sharps}s\""
            flats  > 0 -> "key.sig=\"${flats}f\""
            else       -> "key.sig=\"0\""
        }
    }

    // ── Note state color ──────────────────────────────────────────────────────

    /**
     * Returns the MEI color hex string for a note state, or null for NONE (default black).
     */
    internal fun noteStateMeiColor(state: NoteState): String? = when (state) {
        NoteState.NONE    -> null
        NoteState.CORRECT -> "#2E7D32"
        NoteState.WRONG   -> "#C62828"
        NoteState.LATE    -> "#F9A825"
    }
}
