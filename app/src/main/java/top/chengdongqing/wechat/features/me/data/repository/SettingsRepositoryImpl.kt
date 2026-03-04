package top.chengdongqing.wechat.features.me.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.features.me.domain.repository.SettingsRepository
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore = context.settingsDataStore

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