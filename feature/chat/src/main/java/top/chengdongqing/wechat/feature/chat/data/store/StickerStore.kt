package top.chengdongqing.wechat.feature.chat.data.store

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.designsystem.model.Stickers
import top.chengdongqing.wechat.core.util.toSHA256Hex
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stickerDataStore by preferencesDataStore("managed_stickers")

@Serializable
data class ManagedSticker(val path: String, val isAsset: Boolean)

@Singleton
class StickerStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
    private val assetReferenceManager: AssetReferenceManager
) {
    private val customKey = stringPreferencesKey("custom")
    private val hiddenKey = stringPreferencesKey("hidden")
    private val promotedKey = stringPreferencesKey("promoted")

    val stickers: Flow<List<ManagedSticker>> = context.stickerDataStore.data.map { prefs ->
        val custom = prefs.readList(customKey)
        val hidden = prefs.readList(hiddenKey).toSet()
        val promoted = prefs.readList(promotedKey)
        val assets = Stickers.all.map { it.localPath }.filterNot(hidden::contains)
        val all = custom.map { ManagedSticker(it, false) } +
            assets.map { ManagedSticker(it, true) }
        val rank = promoted.withIndex().associate { it.value to it.index }
        all.distinctBy { it.path }.sortedWith(
            compareBy<ManagedSticker> { rank[it.path] ?: Int.MAX_VALUE }
                .thenBy { all.indexOf(it) }
        )
    }

    suspend fun add(path: String) {
        attach(path)
        context.stickerDataStore.edit { prefs ->
            val custom = prefs.readList(customKey)
            prefs[customKey] = json.encodeToString((listOf(path) + custom).distinct())
            promote(prefs, path)
        }
    }

    suspend fun moveToFront(path: String) = context.stickerDataStore.edit { promote(it, path) }

    suspend fun delete(sticker: ManagedSticker) {
        context.stickerDataStore.edit { prefs ->
            if (sticker.isAsset) {
                prefs[hiddenKey] = json.encodeToString(
                    (prefs.readList(hiddenKey) + sticker.path).distinct()
                )
            } else {
                prefs[customKey] = json.encodeToString(
                    prefs.readList(customKey).filterNot { it == sticker.path }
                )
            }
            prefs[promotedKey] = json.encodeToString(
                prefs.readList(promotedKey).filterNot { it == sticker.path }
            )
        }
        if (!sticker.isAsset) {
            assetReferenceManager.detach(AssetOwner(AssetOwnerType.Sticker, sticker.path))
        }
    }

    /** Registers ownership for stickers saved before the ownership model was introduced. */
    suspend fun preserveIfManaged(path: String?) {
        if (path == null || !File(path).isFile) return
        val custom = context.stickerDataStore.data.first().readList(customKey)
        if (path in custom) attach(path)
    }

    private suspend fun attach(path: String) {
        val file = File(path)
        if (!file.isFile) return
        assetReferenceManager.attach(
            localPath = path,
            checksum = file.toSHA256Hex(),
            owner = AssetOwner(AssetOwnerType.Sticker, path)
        )
    }

    private fun promote(
        prefs: MutablePreferences,
        path: String
    ) {
        prefs[promotedKey] = json.encodeToString(
            (listOf(path) + prefs.readList(promotedKey).filterNot { it == path }).take(100)
        )
    }

    private fun Preferences.readList(
        key: Preferences.Key<String>
    ): List<String> = this[key]?.let {
        runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
    } ?: emptyList()
}
