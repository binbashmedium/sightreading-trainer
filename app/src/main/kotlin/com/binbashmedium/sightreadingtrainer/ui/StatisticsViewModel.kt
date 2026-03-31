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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binbashmedium.sightreadingtrainer.data.SettingsRepository
import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatItemUi(
    val label: String,
    val count: Int
)

data class StatisticsUiState(
    val topCorrectGroups: List<StatItemUi> = emptyList(),
    val topWrongGroups: List<StatItemUi> = emptyList(),
    val topCorrectNotes: List<StatItemUi> = emptyList(),
    val topWrongNotes: List<StatItemUi> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = settingsRepository.settings
        .map { it.toStatisticsUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
}

internal fun AppSettings.toStatisticsUiState(): StatisticsUiState = StatisticsUiState(
    topCorrectGroups = correctGroupStats.toTop5(prettyGroupNames = true),
    topWrongGroups = wrongGroupStats.toTop5(prettyGroupNames = true),
    topCorrectNotes = correctNoteStats.toTop5(prettyGroupNames = false),
    topWrongNotes = wrongNoteStats.toTop5(prettyGroupNames = false)
)

internal fun Map<String, Int>.toTop5(prettyGroupNames: Boolean): List<StatItemUi> =
    entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .take(5)
        .map {
            StatItemUi(
                label = if (prettyGroupNames) it.key.replace('_', ' ') else it.key,
                count = it.value
            )
        }
