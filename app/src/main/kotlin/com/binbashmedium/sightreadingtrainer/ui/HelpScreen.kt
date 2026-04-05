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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HelpScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Help", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // ── Overview ──────────────────────────────────────────────────────────
        HelpSection("Overview") {
            HelpBody(
                "Sightreading Trainer is a real-time music sight-reading app for pianists and " +
                "keyboard players. Connect a USB or Bluetooth MIDI keyboard, start a session, " +
                "and play the notes shown on the grand staff. The app evaluates every note you " +
                "play and colours the noteheads instantly so you can see exactly what you got " +
                "right or wrong."
            )
        }

        // ── Getting Started ───────────────────────────────────────────────────
        HelpSection("Getting Started") {
            HelpBody("1. Connect your MIDI keyboard via USB or Bluetooth.")
            HelpBody("2. Go to Settings and select your MIDI device from the list at the bottom.")
            HelpBody("3. Return to the main screen and tap Start Practice.")
            HelpBody(
                "4. Play each note or chord shown on the staff. The app advances to the next " +
                "step automatically as you play."
            )
            HelpBody(
                "5. A countdown timer is shown in the top-right corner. When time runs out the " +
                "session ends and your score is displayed."
            )
        }

        // ── Practice Screen ───────────────────────────────────────────────────
        HelpSection("Practice Screen") {
            HelpBody(
                "The practice screen shows a grand staff (treble clef on top, bass clef on " +
                "bottom). Notes scroll from right to left and a cursor line marks the current " +
                "expected position."
            )
            HelpBody(
                "Notes above middle C (MIDI 60) are drawn on the treble staff; notes below " +
                "middle C are drawn on the bass staff."
            )
            HelpBody(
                "Ledger lines are added automatically for notes that fall outside the five " +
                "printed staff lines."
            )
            HelpBody(
                "Key signature sharps and flats appear at the beginning of each staff line. " +
                "Accidentals (♯ ♭ ♮) are shown next to individual noteheads when they apply."
            )
            HelpBody(
                "Sustain-pedal marks (Ped. / *) are drawn below the bass staff when a pedal " +
                "exercise step is active."
            )
        }

        // ── Note Colours ──────────────────────────────────────────────────────
        HelpSection("Note Colours") {
            HelpBody("Each notehead is coloured to reflect the evaluation result:")
            Spacer(Modifier.height(8.dp))

            NoteColourRow(
                color = Color.Black,
                label = "Black — not yet played",
                description = "The note has not been reached yet; it is waiting to be played."
            )
            NoteColourRow(
                color = Color(0xFF2E7D32),
                label = "Green — correct",
                description = "You played the correct note or chord within the allowed timing window."
            )
            NoteColourRow(
                color = Color(0xFFC62828),
                label = "Red — wrong or missing",
                description =
                    "An expected note was not played, or you played the wrong pitch. " +
                    "Missing notes that were expected turn red after the step is evaluated."
            )
            NoteColourRow(
                color = Color(0xFFF9A825),
                label = "Yellow — extra / unexpected",
                description =
                    "You played a note that was not part of the expected chord. Extra " +
                    "noteheads appear in yellow on the same beat to show what was played."
            )

            Spacer(Modifier.height(8.dp))
            HelpBody(
                "Stems and beams use the colour of the note they belong to. When a chord " +
                "contains a mix of correct and incorrect notes the stem stays black so it does " +
                "not imply a single overall result."
            )
            HelpBody(
                "Pedal marks follow the same colour scheme: a pedal press or release that was " +
                "played correctly turns green; a missed pedal event turns red; an unexpected " +
                "pedal action appears as an additional yellow mark."
            )
        }

        // ── Exercise Types ────────────────────────────────────────────────────
        HelpSection("Exercise Types") {
            HelpBody(
                "You can enable one or more exercise types in Settings. Each session mixes " +
                "steps from all selected types."
            )
            HelpSubSection("Single Notes") {
                HelpBody(
                    "One note at a time, chosen from the selected key(s). Good for beginners " +
                    "or warming up."
                )
            }
            HelpSubSection("Intervals") {
                HelpBody(
                    "Two notes played simultaneously, spanning a 2nd up to an octave. Trains " +
                    "reading two-voice chords."
                )
            }
            HelpSubSection("Triads") {
                HelpBody(
                    "Three-note chords (root, third, fifth) built from the current key. Both " +
                    "close and open voicings are generated."
                )
            }
            HelpSubSection("Arpeggios") {
                HelpBody(
                    "Broken-chord patterns: the notes of a triad are played one at a time in " +
                    "succession. Useful for practising hand position and finger independence."
                )
            }
            HelpSubSection("Clustered Chords") {
                HelpBody(
                    "Chords with notes close together (seconds and thirds) that may require " +
                    "note displacement in the notation. More challenging to read and play."
                )
            }
            HelpSubSection("Progressions") {
                HelpBody(
                    "Plays named diatonic chord progressions built on the selected key. Each " +
                    "chord in the progression is a separate step you must play in order — the " +
                    "sequence is never shuffled. Enable this type to practise common harmonic " +
                    "patterns and improve chord recognition."
                )
                HelpBody("Available progressions (select one or more in Settings):")
                HelpBody("• I-IV-V-I — classical perfect cadence")
                HelpBody("• I-V-vi-IV — pop progression")
                HelpBody("• ii-V-I — jazz turnaround")
                HelpBody("• I-vi-IV-V — 50s progression")
                HelpBody("• I-IV-I-V — blues turnaround")
                HelpBody("• vi-IV-I-V — minor-feel pop")
                HelpBody("• I-iii-IV-V — ascending walk")
            }
            HelpSubSection("Pedal Exercises") {
                HelpBody(
                    "Notes are combined with sustain-pedal press and release markings. " +
                    "Requires an expression or sustain pedal connected to your MIDI keyboard. " +
                    "Each generated press is always paired with a release later in the exercise."
                )
            }
        }

        // ── Settings Reference ────────────────────────────────────────────────
        HelpSection("Settings") {
            HelpSubSection("Exercise Types") {
                HelpBody(
                    "Multi-select chips. At least one type must remain selected. The session " +
                    "draws steps from all selected types mixed together."
                )
            }
            HelpSubSection("Exercise Time") {
                HelpBody(
                    "Duration of a timed session in seconds (30 – 300 s, default 60 s). " +
                    "When an exercise chunk is completed before the timer expires, a new chunk " +
                    "is generated automatically in the same key so practice continues without " +
                    "interruption."
                )
            }
            HelpSubSection("Exercise Length") {
                HelpBody(
                    "Maximum number of noteheads per exercise chunk (4 – 16, default 8). " +
                    "Chords count each notehead separately toward this budget. Shorter lengths " +
                    "increase repetition; longer lengths give more variety per chunk."
                )
            }
            HelpSubSection("Keys") {
                HelpBody(
                    "Pool of major keys to choose from (C through B, all 12 keys). At least " +
                    "one key must be selected. A key is picked at random from the pool when a " +
                    "new exercise chunk is generated."
                )
            }
            HelpSubSection("Hand Mode") {
                HelpBody("Controls which staff is active for note generation:")
                HelpBody("• RIGHT — treble staff only (notes ≥ middle C).")
                HelpBody("• LEFT — bass staff only (notes < middle C).")
                HelpBody("• BOTH — notes may appear on either staff.")
            }
            HelpSubSection("Chord Progressions") {
                HelpBody(
                    "Visible only when the Progressions exercise type is enabled. " +
                    "Select one or more named progressions (e.g. I-IV-V-I, ii-V-I). " +
                    "At least one must remain selected. Each selected progression " +
                    "contributes its full chord sequence to the exercise in order."
                )
            }
            HelpSubSection("Generated Note Accidentals") {
                HelpBody(
                    "When ON, the generator may include sharps, flats, and natural signs " +
                    "beside noteheads for chromatic pitches. When OFF, all notes are " +
                    "constrained to the natural notes of the selected key — no black keys."
                )
            }
            HelpSubSection("Generated Pedal Marks") {
                HelpBody(
                    "When ON, sustain-pedal press/release steps can be inserted into the " +
                    "exercise. Requires a sustain pedal on your MIDI keyboard. When OFF, no " +
                    "pedal steps are generated."
                )
            }
            HelpSubSection("Ornaments") {
                HelpBody(
                    "Ornamental symbols appear above (or before) notes as decorative performance " +
                    "guides. Select any combination of types; selecting none disables ornaments " +
                    "entirely. When at least one type is active, approximately one in six " +
                    "quarter-note-or-longer steps will receive an ornament. All ornaments are " +
                    "decorative only — only the main note pitch is evaluated during practice."
                )
                HelpBody("How to play each ornament type:")
                HelpBody(
                    "• Trill (tr) — Rapidly alternate between the written note and the note a " +
                    "step above for the full duration of the note. In Baroque style (18th century) " +
                    "start on the upper note; in Classical/Romantic style (19th century) start " +
                    "on the written note."
                )
                HelpBody(
                    "• Upper Mordent (inverted mordent) — Play three notes quickly: written note, " +
                    "the note one step above, then back to the written note. The symbol looks like " +
                    "a short wavy line without a slash."
                )
                HelpBody(
                    "• Lower Mordent — Play three notes quickly: written note, the note one step " +
                    "below, then back to the written note. The symbol looks like a wavy line with " +
                    "a vertical slash through it."
                )
                HelpBody(
                    "• Turn (grupetto) — Play four notes in quick succession: the note one step " +
                    "above the written note, the written note, the note one step below, then back " +
                    "to the written note. The symbol looks like a sideways S above the note."
                )
                HelpBody(
                    "• Appoggiatura — Play the small grace note (shown without a slash) for " +
                    "roughly half the value of the main note with a slight lean, then resolve to " +
                    "the main note for the remaining duration."
                )
                HelpBody(
                    "• Acciaccatura — Play the small slashed grace note as fast as possible, " +
                    "practically together with the main note. It takes no rhythmic value — the " +
                    "main note keeps its full duration."
                )
                HelpBody(
                    "• Arpeggiation — Rather than striking all chord notes at once, roll them " +
                    "rapidly from the lowest to the highest, holding each note as you go. " +
                    "Only applied to chords (multiple simultaneous notes)."
                )
            }
            HelpSubSection("Timing Tolerance") {
                HelpBody(
                    "How many milliseconds before the expected beat a note can be played and " +
                    "still count as correct (50 – 500 ms, default 150 ms). Increase this if " +
                    "correct notes are being marked wrong due to slight early playing."
                )
            }
            HelpSubSection("Chord Detection Window") {
                HelpBody(
                    "How many milliseconds the app waits after the first key press to collect " +
                    "additional notes into the same chord (20 – 200 ms, default 50 ms). " +
                    "Increase this if simultaneous notes in a chord are being split into " +
                    "separate steps."
                )
            }
            HelpSubSection("Sound") {
                HelpBody(
                    "Enables or disables audible feedback tones when a step is evaluated " +
                    "(correct / incorrect sounds)."
                )
            }
            HelpSubSection("MIDI Device") {
                HelpBody(
                    "Selects which connected MIDI device provides input. The list updates " +
                    "automatically when devices are plugged in or out. If no devices appear, " +
                    "check that the keyboard is powered on and the cable or Bluetooth pairing " +
                    "is active."
                )
            }
        }

        // ── Statistics ────────────────────────────────────────────────────────
        HelpSection("Statistics") {
            HelpBody(
                "The Statistics screen shows cumulative totals across all sessions: total " +
                "correct notes, total wrong notes, and your all-time high score."
            )
            HelpBody(
                "It also lists the Top 5 note groups you play most correctly and the Top 5 " +
                "note groups you play most incorrectly. A note group is the set of MIDI note " +
                "numbers in a single expected chord step. Use this to spot patterns in your " +
                "mistakes — for example, a specific interval or chord voicing that you " +
                "consistently miss."
            )
        }

        // ── Scoring ───────────────────────────────────────────────────────────
        HelpSection("Scoring") {
            HelpBody(
                "At the end of each session the finish overlay shows your accuracy (% of " +
                "correctly played notes), the BPM and practice time, and a composite highscore. " +
                "The highscore formula is: accuracy × BPM-factor × time-factor × 1000, where " +
                "BPM-factor = BPM / 120 (capped at 240 BPM) and time-factor = minutes practiced " +
                "(capped at 5 min). Your all-time highscore is saved automatically."
            )
        }

        // ── MIDI Reconnect ────────────────────────────────────────────────────
        HelpSection("MIDI Reconnect") {
            HelpBody(
                "If your MIDI keyboard is disconnected during a session the app detects the " +
                "removal and waits. When the same device is reconnected it is opened " +
                "automatically and the session resumes without any manual action."
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back") }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Spacer(Modifier.height(4.dp))
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        content()
    }
}

@Composable
private fun HelpSubSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
    Column(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
        content()
    }
}

@Composable
private fun HelpBody(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun NoteColourRow(color: Color, label: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
                .background(color = color, shape = RoundedCornerShape(50))
        )
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
