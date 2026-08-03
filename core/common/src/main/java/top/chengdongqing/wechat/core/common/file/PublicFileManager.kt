package top.chengdongqing.wechat.core.common.file

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.model.MessageType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import top.chengdongqing.wechat.core.designsystem.R as DesignR

/**
 * 系统 MediaStore 文件管理器
 */
@Singleton
class PublicFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PublicFileManager"
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    private val appName: String
        get() = context.getString(DesignR.string.app_name)

    /**
     * 保存媒体文件到 MediaStore（从 Uri）
     *
     * @param messageType 消息类型，决定写入哪个 MediaStore 集合
     * @param sourceUri   源文件 Uri（content:// 或 file://）
     * @param filename    目标文件名
     * @return 成功返回写入后的 Uri，失败返回 null
     */
    suspend fun saveMedia(
        messageType: MessageType,
        sourceUri: Uri,
        filename: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val config = resolveConfig(messageType, filename, sourceUri = sourceUri)
            writeToMediaStore(config, sourceUri = sourceUri)
        }.onFailure { Log.e(TAG, "saveMedia(Uri) 失败", it) }
            .getOrNull()
    }

    /**
     * 保存媒体文件到 MediaStore（从 File）
     *
     * @param messageType 消息类型
     * @param sourceFile  源文件
     * @param filename    目标文件名
     * @return 成功返回写入后的 Uri，失败返回 null
     */
    suspend fun saveMedia(
        messageType: MessageType,
        sourceFile: File,
        filename: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists()) error("源文件不存在: ${sourceFile.absolutePath}")
            val config = resolveConfig(messageType, filename, sourceFile = sourceFile)
            writeToMediaStore(config, sourceFile = sourceFile)
        }.onFailure { Log.e(TAG, "saveMedia(File) 失败", it) }
            .getOrNull()
    }

    /**
     * 向 MediaStore 写入文件（IS_PENDING 保护）
     *
     * 只传 sourceUri 或 sourceFile 其中一个
     */
    private fun writeToMediaStore(
        config: MediaConfig,
        sourceUri: Uri? = null,
        sourceFile: File? = null
    ): Uri? {
        val tempUri = contentResolver.insert(config.collectionUri, config.contentValues)
            ?: return null

        val success = try {
            when {
                sourceFile != null -> copyFile(sourceFile, tempUri)
                sourceUri != null -> copyUri(sourceUri, tempUri)
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "文件复制失败", e)
            false
        }

        return if (success) {
            finishPending(tempUri)
            tempUri
        } else {
            contentResolver.delete(tempUri, null, null)
            null
        }
    }

    /**
     * 根据 MessageType 解析 MediaStore 写入配置
     */
    private suspend fun resolveConfig(
        messageType: MessageType,
        filename: String?,
        sourceUri: Uri? = null,
        sourceFile: File? = null
    ): MediaConfig {
        val fileConfig = messageType.getFileConfig()

        val finalExtension = filename.extractExtension() ?: run {
            if (sourceUri != null) {
                context.getFileMetadata(sourceUri)?.filename.extractExtension()
            } else {
                sourceFile?.extension
            }
        } ?: fileConfig.extension

        // 确定文件名
        val finalFilename = when {
            // 是文件类型时，以传入的文件名为主
            messageType == MessageType.File && !filename.isNullOrBlank() -> filename
            // 其余情况都重新生成文件名
            else -> generateFileName(fileConfig.prefix, finalExtension)
        }

        // 确定 MIME 类型
        val mimeType = when {
            sourceUri != null -> contentResolver.getType(sourceUri)
                ?: guessMimeType(finalFilename)

            sourceFile != null -> guessMimeType(finalFilename)
            else -> guessMimeType(finalFilename)
        }

        return when (messageType) {
            MessageType.Image,
            MessageType.Sticker -> MediaConfig(
                collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues = buildContentValues(
                    displayName = finalFilename,
                    mimeType = mimeType,
                    relativePath = "${Environment.DIRECTORY_PICTURES}/$appName"
                )
            )

            MessageType.Video -> MediaConfig(
                collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues = buildContentValues(
                    displayName = finalFilename,
                    mimeType = mimeType,
                    relativePath = "${Environment.DIRECTORY_MOVIES}/$appName"
                )
            )

            MessageType.Voice -> {
                val parentDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Environment.DIRECTORY_RECORDINGS
                } else {
                    Environment.DIRECTORY_MUSIC
                }

                MediaConfig(
                    collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    contentValues = buildContentValues(
                        displayName = finalFilename,
                        mimeType = mimeType,
                        relativePath = "${parentDir}/$appName"
                    )
                )
            }

            // File 及其他类型 → Downloads
            else -> MediaConfig(
                collectionUri = getDownloadsCollectionUri(),
                contentValues = buildDownloadsContentValues(finalFilename, mimeType)
            )
        }
    }

    /**
     * 通用 ContentValues（图片/视频/音频）
     */
    private fun buildContentValues(
        displayName: String,
        mimeType: String,
        relativePath: String
    ): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    /**
     * 下载目录 ContentValues（兼容 Android 9）
     */
    private fun buildDownloadsContentValues(
        displayName: String,
        mimeType: String
    ): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.IS_PENDING, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$appName"
            )
            // 标记为下载文件，系统自动处理同名冲突（追加 (1)(2)...）
            put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
        } else {
            // Android 9 及以下需要完整物理路径
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val targetDir = File(downloadsDir, appName).also { it.mkdirs() }
            put(MediaStore.MediaColumns.DATA, "${targetDir.absolutePath}/$displayName")
        }
    }

    private fun getDownloadsCollectionUri(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

    private fun finishPending(uri: Uri) {
        contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null
        )
    }

    private fun copyUri(from: Uri, to: Uri): Boolean = try {
        contentResolver.openInputStream(from)?.use { input ->
            contentResolver.openOutputStream(to)?.use { output ->
                input.copyTo(output)
                true
            }
        } ?: false
    } catch (e: Exception) {
        Log.e(TAG, "copyUri 失败", e)
        false
    }

    private fun copyFile(from: File, to: Uri): Boolean = try {
        from.inputStream().use { input ->
            contentResolver.openOutputStream(to)?.use { output ->
                input.copyTo(output)
                true
            }
        } ?: false
    } catch (e: Exception) {
        Log.e(TAG, "copyFile 失败", e)
        false
    }

    private fun guessMimeType(filename: String): String {
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "mp3" -> "Voice/mpeg"
            "m4a" -> "Voice/mp4"
            "wav" -> "Voice/wav"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}

private data class MediaConfig(
    val collectionUri: Uri,
    val contentValues: ContentValues
)
