package top.chengdongqing.wechat.feature.chat.data.store

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.MusicTrack
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.util.toSHA256Hex
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicLibraryStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetReferenceManager: AssetReferenceManager
) {
    suspend fun add(track: MusicTrack) {
        attach(track.audioPath)
        attach(track.coverPath)
    }

    suspend fun delete(track: MusicTrack) {
        // Backfill ownership first for tracks imported by older app versions.
        add(track)
        detach(track.audioPath)
        detach(track.coverPath)
    }

    /** Registers ownership for music imported before the ownership model was introduced. */
    suspend fun preserveIfManaged(path: String?) {
        if (path == null || !File(path).isFile) return
        if (loadTracks().any { it.audioPath == path || it.coverPath == path }) attach(path)
    }

    private fun loadTracks(): List<MusicTrack> {
        val value = context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
            .getString(MUSIC_ITEMS, null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<MusicTrack>>(value) }.getOrDefault(emptyList())
    }

    private suspend fun attach(path: String?) {
        val file = path?.let(::File) ?: return
        if (!file.isFile) return
        assetReferenceManager.attach(
            localPath = path,
            checksum = file.toSHA256Hex(),
            owner = AssetOwner(AssetOwnerType.Music, path)
        )
    }

    private suspend fun detach(path: String?) {
        if (path == null) return
        assetReferenceManager.detach(AssetOwner(AssetOwnerType.Music, path))
    }

    private companion object {
        const val MUSIC_PREFS = "local_music_library"
        const val MUSIC_ITEMS = "items"
    }
}
