package top.chengdongqing.wechat.features.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.PrivacySettingsDataStore
import top.chengdongqing.wechat.features.settings.domain.repository.PrivacySettingsRepository
import javax.inject.Inject

class PrivacySettingsRepositoryImpl @Inject constructor(
    @param:PrivacySettingsDataStore private val dataStore: DataStore<Preferences>
) : PrivacySettingsRepository {

    private companion object {
        val FRIEND_VERIFY_KEY = booleanPreferencesKey("friend_verify_enabled")
    }

    override val friendVerifyEnabled: Flow<Boolean> = dataStore.data
        .map { it[FRIEND_VERIFY_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun toggleFriendVerify(enabled: Boolean) {
        dataStore.edit { it[FRIEND_VERIFY_KEY] = enabled }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}