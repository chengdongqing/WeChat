package top.chengdongqing.wechat.features.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.NotificationSettingsDataStore
import top.chengdongqing.wechat.features.settings.domain.model.NotificationDisplay
import top.chengdongqing.wechat.features.settings.domain.model.NotificationSound
import top.chengdongqing.wechat.features.settings.domain.model.RingtoneSound
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository

class NotificationSettingsRepositoryImpl @Inject constructor(
    @param:NotificationSettingsDataStore private val dataStore: DataStore<Preferences>
) : NotificationSettingsRepository {

    private companion object {
        val MSG_NOTIFICATION_KEY = booleanPreferencesKey("msg_notification_enabled")
        val CALL_NOTIFICATION_KEY = booleanPreferencesKey("call_notification_enabled")
        val IN_CHAT_SOUND_KEY = booleanPreferencesKey("in_chat_sound_enabled")
        val IN_CHAT_VIBRATION_KEY = booleanPreferencesKey("in_chat_vibration_enabled")
        val NOTIFICATION_DISPLAY_KEY = stringPreferencesKey("notification_display")
        val NOTIFICATION_SOUND_KEY = stringPreferencesKey("notification_sound")
        val RINGTONE_KEY = stringPreferencesKey("ringtone")
        val RINGTONE_AUDIBLE_KEY = booleanPreferencesKey("ringtone_audible_enabled")
    }

    override val msgNotificationEnabled: Flow<Boolean> = dataStore.data
        .map { it[MSG_NOTIFICATION_KEY] ?: true }
        .distinctUntilChanged()

    override val callNotificationEnabled: Flow<Boolean> = dataStore.data
        .map { it[CALL_NOTIFICATION_KEY] ?: true }
        .distinctUntilChanged()

    override val inChatSoundEnabled: Flow<Boolean> = dataStore.data
        .map { it[IN_CHAT_SOUND_KEY] ?: true }
        .distinctUntilChanged()

    override val inChatVibrationEnabled: Flow<Boolean> = dataStore.data
        .map { it[IN_CHAT_VIBRATION_KEY] ?: true }
        .distinctUntilChanged()

    override val notificationDisplay: Flow<NotificationDisplay> = dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_DISPLAY_KEY]
                ?.let { runCatching { NotificationDisplay.valueOf(it) }.getOrNull() }
                ?: NotificationDisplay.SenderAndContent
        }
        .distinctUntilChanged()

    override val notificationSound: Flow<NotificationSound> = dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_SOUND_KEY]
                ?.let { runCatching { NotificationSound.valueOf(it) }.getOrNull() }
                ?: NotificationSound.FollowSystem
        }
        .distinctUntilChanged()

    override val ringtone: Flow<RingtoneSound> = dataStore.data
        .map { preferences ->
            preferences[RINGTONE_KEY]
                ?.let { runCatching { RingtoneSound.valueOf(it) }.getOrNull() }
                ?: RingtoneSound.Default
        }
        .distinctUntilChanged()

    override val ringtoneAudibleEnabled: Flow<Boolean> = dataStore.data
        .map { it[RINGTONE_AUDIBLE_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun toggleMsgNotification(enabled: Boolean) {
        dataStore.edit { it[MSG_NOTIFICATION_KEY] = enabled }
    }

    override suspend fun toggleCallNotification(enabled: Boolean) {
        dataStore.edit { it[CALL_NOTIFICATION_KEY] = enabled }
    }

    override suspend fun toggleInChatSound(enabled: Boolean) {
        dataStore.edit { it[IN_CHAT_SOUND_KEY] = enabled }
    }

    override suspend fun toggleInChatVibration(enabled: Boolean) {
        dataStore.edit { it[IN_CHAT_VIBRATION_KEY] = enabled }
    }

    override suspend fun setNotificationDisplay(display: NotificationDisplay) {
        dataStore.edit { it[NOTIFICATION_DISPLAY_KEY] = display.name }
    }

    override suspend fun setNotificationSound(sound: NotificationSound) {
        dataStore.edit { it[NOTIFICATION_SOUND_KEY] = sound.name }
    }

    override suspend fun setRingtone(ringtone: RingtoneSound) {
        dataStore.edit { it[RINGTONE_KEY] = ringtone.name }
    }

    override suspend fun toggleRingtoneAudible(enabled: Boolean) {
        dataStore.edit { it[RINGTONE_AUDIBLE_KEY] = enabled }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}