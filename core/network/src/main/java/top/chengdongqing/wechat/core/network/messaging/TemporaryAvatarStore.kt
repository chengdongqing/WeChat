package top.chengdongqing.wechat.core.network.messaging

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.data.storage.AssetOwner
import top.chengdongqing.wechat.core.data.storage.AssetOwnerType
import top.chengdongqing.wechat.core.data.storage.AssetReferenceManager
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.util.downloadAvatar
import top.chengdongqing.wechat.core.util.toSHA256Hex
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the short-lived LAN avatar URL used by a temporary chat. */
@Singleton
class TemporaryAvatarStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val privateFileManager: PrivateFileManager,
    private val assetReferenceManager: AssetReferenceManager
) {
    suspend fun persist(peerId: String, source: String?): String? {
        if (source.isNullOrBlank()) return null

        val sourceFile = File(source)
        val localPath = if (sourceFile.isFile) {
            sourceFile.absolutePath
        } else {
            val download = withContext(Dispatchers.IO) {
                File.createTempFile("temp_avatar_", ".jpg", context.cacheDir)
            }
            try {
                downloadAvatar(source, download).getOrThrow()
                privateFileManager.saveAvatar(peerId, download.toUri()).getOrThrow()
            } finally {
                download.delete()
            }
        }

        assetReferenceManager.attach(
            localPath = localPath,
            checksum = File(localPath).toSHA256Hex(),
            owner = AssetOwner(AssetOwnerType.ChatSession, peerId)
        )
        return localPath
    }
}
