package top.chengdongqing.wechat.core.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType
import java.io.File
import java.io.IOException

/**
 * 创建 MediaStore 插入所需的 ContentValues
 * 核心逻辑：设置文件名、路径并开启 [MediaStore.MediaColumns.IS_PENDING] 状态
 */
fun Context.createMediaContentValues(
    filename: String,
    mimeType: String,
    mediaType: MediaType,
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
    val appName = getString(R.string.app_name)
    val relativePath = "$directory/$appName"

    return buildMediaContentValues(filename, mimeType, relativePath)
}

private fun buildMediaContentValues(
    filename: String,
    mimeType: String,
    relativePath: String,
): ContentValues =
    ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

/**
 * 文件写入完成后，取消挂起状态，使媒体文件在相册中可见
 */
fun Context.finishPending(uri: Uri) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.IS_PENDING, 0)
    }
    contentResolver.update(uri, contentValues, null, null)
}

/**
 * 根据媒体类型获取对应的 MediaStore 系统表 Uri
 */
fun getContentUri(mediaType: MediaType): Uri =
    when (mediaType) {
        MediaType.Image -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaType.Video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        MediaType.Audio, MediaType.Recording -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

/**
 * 加载媒体缩略图（兼容图片与视频）
 * @return 可能是 Uri (低版本图片), Bitmap (高版本或视频) 或 null
 */
suspend fun Context.loadMediaThumbnail(
    uri: Uri,
    isVideo: Boolean = false,
    size: Size = Size(200, 200)
): Any? {
    // Android 10 以下的图片，直接返回原图 Uri
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !isVideo) {
        return uri
    }

    return withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ 官方推荐的缩略图加载方式，系统会自动处理缓存
                contentResolver.loadThumbnail(uri, size, null)
            } else {
                // API 29 以下视频文件需要手动提取首帧
                loadVideoThumbnail(uri)
            }
        } catch (_: IOException) {
            if (!isVideo) {
                uri
            } else {
                loadVideoThumbnail(uri)
            }
        }
    }
}

/**
 * 针对低版本系统的视频首帧提取
 */
fun Context.loadVideoThumbnail(uri: Uri): Bitmap? {
    return MediaMetadataRetriever().use { retriever ->
        try {
            retriever.setDataSource(this, uri)
            // 提取第一秒或第一帧（关键帧），OPTION_CLOSEST_SYNC 性能较平衡
            retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * 跨 URI 的数据流拷贝（用于文件保存或导出）
 */
fun ContentResolver.copyUri(from: Uri, to: Uri): Boolean {
    return try {
        openInputStream(from)?.use { input ->
            openOutputStream(to)?.use { output ->
                input.copyTo(output)
                true
            }
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * 保存媒体文件到相册
 */
suspend fun Context.saveToAlbum(media: MediaItem): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val contentUri = getContentUri(media.mediaType)
        val contentValues = createMediaContentValues(
            media.filename,
            media.mimeType,
            media.mediaType
        )

        // 插入数据库记录（此时文件是 IS_PENDING = 1 状态）
        val tempUri = contentResolver.insert(contentUri, contentValues) ?: return@withContext false

        // 拷贝数据流
        val isSuccess = contentResolver.copyUri(media.uri, tempUri)

        if (isSuccess) {
            // 成功：解除挂起状态，让相册可见
            finishPending(tempUri)
            true
        } else {
            // 失败：删除数据库中的占位记录，防止相册出现空白文件
            contentResolver.delete(tempUri, null, null)
            false
        }
    }.onFailure { e ->
        Log.e("MediaStoreExt", "保存媒体文件到相册", e)
    }.getOrDefault(false)
}

/**
 * 保存媒体文件到相册
 */
suspend fun Context.saveToAlbum(uri: Uri): Boolean {
    val res = prepareMediaResource(this, uri) ?: return false
    return saveToAlbum(res.toMediaItem(uri))
}

/**
 * 把 content:// Uri 复制到应用私有目录，返回真实路径
 * 用于消息发送前将媒体文件持久化到本地
 */
suspend fun Context.copyUriToPrivateDir(
    uri: Uri,
    mediaType: MediaType
): String? = withContext(Dispatchers.IO) {
    try {
        val subDir = when (mediaType) {
            MediaType.Image -> "images"
            MediaType.Video -> "videos"
            MediaType.Audio,
            MediaType.Recording -> "voices"
        }

        val dir = File(filesDir, subDir).also { it.mkdirs() }

        val fileName = "${randomUUID()}_${getFileName(uri)}"
        val destFile = File(dir, fileName)
        val success = contentResolver.copyUri(uri, Uri.fromFile(destFile))

        if (success) destFile.absolutePath else null
    } catch (e: Exception) {
        Log.e("MediaStoreExt", "复制文件失败", e)
        null
    }
}

/**
 * 获取 Uri 对应的文件名
 */
fun Context.getFileName(uri: Uri): String {
    var name = "${System.currentTimeMillis()}"
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) {
            name = cursor.getString(index)
        }
    }
    return name
}