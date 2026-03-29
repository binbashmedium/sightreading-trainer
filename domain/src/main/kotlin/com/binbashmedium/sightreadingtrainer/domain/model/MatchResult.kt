package com.binbashmedium.sightreadingtrainer.domain.model

sealed class MatchResult {
    object Correct : MatchResult()
    object Incorrect : MatchResult()
    object TooEarly : MatchResult()
    object TooLate : MatchResult()
    object Waiting : MatchResult()
}
