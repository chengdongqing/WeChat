package top.chengdongqing.wechat.features.me.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.ChatSettingsDataStore
import top.chengdongqing.wechat.features.me.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    @param:ChatSettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val SPEAKER_KEY = booleanPreferencesKey("speaker_enabled")
    }

    override val speakerEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SPEAKER_KEY] ?: true
        }
        .distinctUntilChanged()

    override suspend fun toggleSpeaker(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SPEAKER_KEY] = enabled
        }
    }
}