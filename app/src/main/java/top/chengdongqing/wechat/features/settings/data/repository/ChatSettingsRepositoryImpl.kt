package top.chengdongqing.wechat.features.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.ChatSettingsDataStore
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import javax.inject.Inject

class ChatSettingsRepositoryImpl @Inject constructor(
    @param:ChatSettingsDataStore private val dataStore: DataStore<Preferences>
) : ChatSettingsRepository {

    private companion object {
        val SPEAKER_KEY = booleanPreferencesKey("speaker_enabled")
        val SEND_BUTTON_KEY = booleanPreferencesKey("send_button_enabled")
        val E2E_KEY = booleanPreferencesKey("e2e_enabled")
        val CHAT_BACKGROUND_KEY = stringPreferencesKey("chat_background")
    }

    override val speakerEnabled: Flow<Boolean> = dataStore.data
        .map { it[SPEAKER_KEY] ?: true }
        .distinctUntilChanged()

    override val sendButtonEnabled: Flow<Boolean> = dataStore.data
        .map { it[SEND_BUTTON_KEY] ?: true }
        .distinctUntilChanged()

    override val e2eEnabled: Flow<Boolean> = dataStore.data
        .map { it[E2E_KEY] ?: true }
        .distinctUntilChanged()

    override val chatBackground: Flow<String?> = dataStore.data
        .map { it[CHAT_BACKGROUND_KEY] }
        .distinctUntilChanged()

    override suspend fun toggleSpeaker(enabled: Boolean) {
        dataStore.edit { it[SPEAKER_KEY] = enabled }
    }

    override suspend fun toggleSendButton(enabled: Boolean) {
        dataStore.edit { it[SEND_BUTTON_KEY] = enabled }
    }

    override suspend fun toggleE2e(enabled: Boolean) {
        dataStore.edit { it[E2E_KEY] = enabled }
    }

    override suspend fun setChatBackground(path: String?) {
        dataStore.edit { preferences ->
            if (path != null) {
                preferences[CHAT_BACKGROUND_KEY] = path
            } else {
                preferences.remove(CHAT_BACKGROUND_KEY)
            }
        }
    }
}