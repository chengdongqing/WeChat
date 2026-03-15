package top.chengdongqing.wechat.features.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.di.ConnectionSettingsDataStore
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import javax.inject.Inject

class ConnectionSettingsRepositoryImpl @Inject constructor(
    @param:ConnectionSettingsDataStore private val dataStore: DataStore<Preferences>
) : ConnectionSettingsRepository {

    private companion object {
        val CONNECTION_MODE_KEY = stringPreferencesKey("connection_mode")
    }

    override val connectionMode: Flow<ConnectionMode> = dataStore.data
        .map { ConnectionMode.fromName(it[CONNECTION_MODE_KEY]) }
        .distinctUntilChanged()

    override suspend fun setConnectionMode(mode: ConnectionMode) {
        dataStore.edit { it[CONNECTION_MODE_KEY] = mode.name }
    }
}