package top.chengdongqing.wechat.features.chat.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recentEmojisDataStore by preferencesDataStore("recent_emojis")

@Singleton
class RecentEmojisStore @Inject constructor(
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val KEY = stringPreferencesKey("recent_emojis")
        const val MAX_COUNT = 8
    }

    /**
     * 读取最近使用的表情 key 列表（有序，最新在前）
     */
    suspend fun getRecentEmojis(): List<String> {
        return context.recentEmojisDataStore.data.first()
            .let { prefs ->
                prefs[KEY]
                    ?.let {
                        runCatching { json.decodeFromString<List<String>>(it) }
                            .getOrDefault(emptyList())
                    }
                    ?: emptyList()
            }
    }

    /**
     * 记录一次使用
     */
    suspend fun record(emojiKey: String) {
        context.recentEmojisDataStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let {
                    runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(
                        emptyList()
                    )
                }
                ?: emptyList()

            val updated = (listOf(emojiKey) + current.filter { it != emojiKey })
                .take(MAX_COUNT)

            prefs[KEY] = json.encodeToString(updated)
        }
    }
}