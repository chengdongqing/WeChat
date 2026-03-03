package top.chengdongqing.wechat.core.file

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
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统相册和下载目录管理器
 *
 * 职责：
 * - 保存文件到系统相册（MediaStore）
 * - 保存文件到下载目录（MediaStore.Downloads）
 * - 管理 ContentValues 和 IS_PENDING 状态
 * - 自动处理同名文件
 */
@Singleton
class MediaStoreManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    /**
     * 保存文件到系统相册
     *
     * @param sourceUri 源文件 Uri
     * @param filename 文件名
     * @param mimeType MIME 类型
     * @param mediaType 媒体类型
     * @return 成功返回 true
     */
    suspend fun saveToAlbum(
        sourceUri: Uri,
        filename: String,
        mimeType: String,
        mediaType: MediaType
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val contentUri = getAlbumContentUri(mediaType)
            val contentValues = createAlbumContentValues(filename, mimeType, mediaType)

            /**
             * 插入数据库记录（IS_PENDING = 1）
             */
            val tempUri = contentResolver.insert(contentUri, contentValues)
                ?: return@withContext false

            /**
             * 复制文件内容
             */
            val isSuccess = copyUri(sourceUri, tempUri)

            if (isSuccess) {
                /**
                 * 成功：解除挂起状态，让文件立即可见
                 */
                finishPending(tempUri)
                true
            } else {
                /**
                 * 失败：删除占位记录
                 */
                contentResolver.delete(tempUri, null, null)
                false
            }
        }.onFailure { e ->
            Log.e(TAG, "保存到相册失败", e)
        }.getOrDefault(false)
    }

    /**
     * 保存文件到下载目录
     *
     * 使用 MediaStore API 确保文件立即可见
     * 自动处理同名文件冲突
     *
     * @param sourceUri 源文件 Uri（可以是 content:// 或 file://）
     * @param filename 文件名
     * @param mimeType MIME 类型（可选，会自动推断）
     * @return 成功返回 true
     */
    suspend fun saveToDownloads(
        sourceUri: Uri,
        filename: String,
        mimeType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            /**
             * 推断 MIME 类型
             */
            val finalMimeType = mimeType
                ?: contentResolver.getType(sourceUri)
                ?: guessMimeType(filename)

            val contentUri = getDownloadsContentUri()
            val contentValues = createDownloadsContentValues(filename, finalMimeType)

            /**
             * 插入数据库记录（IS_PENDING = 1）
             */
            val tempUri = contentResolver.insert(contentUri, contentValues)
                ?: return@withContext false

            /**
             * 复制文件内容
             */
            val isSuccess = if (sourceUri.scheme == "file") {
                copyFile(File(sourceUri.path!!), tempUri)
            } else {
                copyUri(sourceUri, tempUri)
            }

            if (isSuccess) {
                /**
                 * 成功：解除挂起状态，文件立即可见
                 */
                finishPending(tempUri)
                true
            } else {
                /**
                 * 失败：删除占位记录
                 */
                contentResolver.delete(tempUri, null, null)
                false
            }
        }.onFailure { e ->
            Log.e(TAG, "保存到下载目录失败", e)
        }.getOrDefault(false)
    }

    /**
     * 保存 File 到下载目录
     *
     * @param sourceFile 源文件
     * @param filename 文件名（可选，默认使用源文件名）
     * @param mimeType MIME 类型（可选）
     */
    suspend fun saveToDownloads(
        sourceFile: File,
        filename: String? = null,
        mimeType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists()) {
                return@withContext false
            }

            val finalFilename = filename ?: sourceFile.name
            val finalMimeType = mimeType ?: guessMimeType(finalFilename)

            val contentUri = getDownloadsContentUri()
            val contentValues = createDownloadsContentValues(finalFilename, finalMimeType)

            val tempUri = contentResolver.insert(contentUri, contentValues)
                ?: return@withContext false

            val isSuccess = copyFile(sourceFile, tempUri)

            if (isSuccess) {
                finishPending(tempUri)
                true
            } else {
                contentResolver.delete(tempUri, null, null)
                false
            }
        }.onFailure { e ->
            Log.e(TAG, "保存文件到下载目录失败", e)
        }.getOrDefault(false)
    }

    /**
     * 创建相册 ContentValues
     */
    private fun createAlbumContentValues(
        filename: String,
        mimeType: String,
        mediaType: MediaType
    ): ContentValues {
        val directory = when (mediaType) {
            MediaType.Image -> Environment.DIRECTORY_PICTURES
            MediaType.Video -> Environment.DIRECTORY_MOVIES
            MediaType.Audio -> Environment.DIRECTORY_MUSIC
            MediaType.Recording -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Environment.DIRECTORY_RECORDINGS
            } else {
                Environment.DIRECTORY_MUSIC
            }
        }

        val appName = context.getString(R.string.app_name)
        val relativePath = "$directory/$appName"

        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    /**
     * 创建下载目录 ContentValues
     */
    private fun createDownloadsContentValues(
        filename: String,
        mimeType: String
    ): ContentValues {
        val appName = context.getString(R.string.app_name)

        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /**
                 * Android 10+ 使用 RELATIVE_PATH
                 */
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$appName"
                )
            } else {
                /**
                 * Android 9 及以下需要手动构建完整路径
                 */
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val targetPath = File(downloadsDir, appName).absolutePath
                put(MediaStore.MediaColumns.DATA, "$targetPath/$filename")
            }

            put(MediaStore.MediaColumns.IS_PENDING, 1)

            /**
             * 同名文件自动处理
             * 系统会自动在文件名后添加 (1), (2) 等后缀
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
            }
        }
    }

    /**
     * 完成挂起状态
     *
     * 将 IS_PENDING 设为 0，使文件立即可见
     */
    private fun finishPending(uri: Uri) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        contentResolver.update(uri, contentValues, null, null)
    }

    /**
     * 获取相册 ContentUri
     */
    private fun getAlbumContentUri(mediaType: MediaType): Uri {
        return when (mediaType) {
            MediaType.Image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.Audio, MediaType.Recording -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    /**
     * 获取下载目录 ContentUri
     */
    private fun getDownloadsContentUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /**
             * Android 10+ 使用专门的 Downloads Uri
             */
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            /**
             * Android 9 及以下使用 Files Uri
             */
            MediaStore.Files.getContentUri("external")
        }
    }

    /**
     * Uri 到 Uri 的复制
     */
    private fun copyUri(from: Uri, to: Uri): Boolean {
        return try {
            contentResolver.openInputStream(from)?.use { input ->
                contentResolver.openOutputStream(to)?.use { output ->
                    input.copyTo(output)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "复制 Uri 失败", e)
            false
        }
    }

    /**
     * File 到 Uri 的复制
     */
    private fun copyFile(from: File, to: Uri): Boolean {
        return try {
            from.inputStream().use { input ->
                contentResolver.openOutputStream(to)?.use { output ->
                    input.copyTo(output)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "复制文件失败", e)
            false
        }
    }

    /**
     * 根据文件名推断 MIME 类型
     */
    private fun guessMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // 图片
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"

            // 视频
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"

            // 音频
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"

            // 文档
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"

            // 压缩包
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"

            // APK
            "apk" -> "application/vnd.android.package-archive"

            // 默认
            else -> "application/octet-stream"
        }
    }

    companion object {
        private const val TAG = "MediaStoreManager"
    }
}