package com.binbashmedium.sightreadingtrainer.domain.model

/**
 * Rhythmic duration of a note or chord as displayed on the staff.
 *
 * This affects how the note head is drawn (open vs filled) and whether a
 * stem and/or flag is attached.  It does not currently affect the timing
 * tolerance used during note matching, but it could be used in future
 * beat-synchronised exercise modes.
 *
 *   WHOLE   = open head, no stem         (4 beats in 4/4)
 *   HALF    = open head, with stem       (2 beats)
 *   QUARTER = filled head, with stem     (1 beat)
 *   EIGHTH  = filled head, stem + flag   (½ beat)
 */
enum class NoteValue {
    WHOLE,
    HALF,
    QUARTER,
    EIGHTH;

    /** Number of beats this value occupies in 4/4 time. */
    val beats: Float get() = when (this) {
        WHOLE   -> 4f
        HALF    -> 2f
        QUARTER -> 1f
        EIGHTH  -> 0.5f
    }
}
