package com.binbashmedium.sightreadingtrainer.data

import com.binbashmedium.sightreadingtrainer.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    val settings: Flow<AppSettings> = dataStore.settings

    suspend fun save(settings: AppSettings) = dataStore.updateSettings(settings)
}
