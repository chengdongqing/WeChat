package top.chengdongqing.wechat.core.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ProfileSettings(private val context: Context) {

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR = stringPreferencesKey("avatar")
    }

    val nicknameFlow = context.dataStore.data.map { it[Keys.NICKNAME] ?: "微信用户" }
    val avatarFlow = context.dataStore.data.map { it[Keys.AVATAR] ?: "" }

    suspend fun updateNickname(name: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = name }
    }

    suspend fun updateAvatar(avatar: String) {
        context.dataStore.edit { it[Keys.AVATAR] = avatar }
    }
}