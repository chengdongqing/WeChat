package top.chengdongqing.wechat.core.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 加载媒体缩略图
 *
 * 兼容图片和视频，自动选择最优方案
 *
 * @param uri 媒体 Uri
 * @param isVideo 是否为视频
 * @param size 缩略图大小
 * @return Uri（低版本图片）或 Bitmap，失败返回 null
 */
suspend fun Context.loadMediaThumbnail(
    uri: Uri,
    isVideo: Boolean = false,
    size: Size = Size(200, 200)
): Any? {
    /**
     * Android 10 以下的图片直接返回原图 Uri
     */
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !isVideo) {
        return uri
    }

    return withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /**
                 * API 29+ 使用系统缩略图加载
                 * 系统会自动处理缓存
                 */
                contentResolver.loadThumbnail(uri, size, null)
            } else {
                /**
                 * API 29 以下手动提取视频首帧
                 */
                loadVideoThumbnail(uri)
            }
        } catch (_: IOException) {
            if (!isVideo) uri else loadVideoThumbnail(uri)
        }
    }
}

/**
 * 提取视频首帧
 *
 * 用于低版本系统
 */
fun Context.loadVideoThumbnail(uri: Uri): Bitmap? {
    return MediaMetadataRetriever().use { retriever ->
        try {
            retriever.setDataSource(this, uri)
            retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * 复制 Uri 到私有目录
 *
 * 将 content:// Uri 复制到应用私有目录，返回绝对路径
 * 用于消息发送前持久化媒体文件
 *
 * @param uri 源 Uri
 * @param subDir 子目录名（如 "images", "videos"）
 * @return 文件绝对路径，失败返回 null
 */
suspend fun Context.copyUriToPrivateDir(
    uri: Uri,
    subDir: String
): String? = withContext(Dispatchers.IO) {
    try {
        val dir = File(filesDir, subDir).also { it.mkdirs() }
        val fileName = "${randomUUID()}_${getFileName(uri)}"
        val destFile = File(dir, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null

        destFile.absolutePath
    } catch (e: Exception) {
        Log.e("FileCopy", "复制文件失败", e)
        null
    }
}

/**
 * 复制 Asset 文件到私有目录
 *
 * @param assetName Asset 文件名
 * @return 文件对象
 */
suspend fun Context.copyAssetToFile(assetName: String): File =
    withContext(Dispatchers.IO) {
        val file = File(filesDir, assetName)
        if (file.exists()) return@withContext file

        file.parentFile?.mkdirs()

        assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        file
    }

/**
 * 从 Bitmap 创建临时 Uri
 *
 * @param bitmap 位图
 * @param quality 压缩质量（0-100）
 * @return FileProvider Uri
 */
suspend fun Context.createImageUri(
    bitmap: Bitmap,
    quality: Int = 90
): Uri = withContext(Dispatchers.IO) {
    val tempFile = File.createTempFile("IMG_", ".jpg", cacheDir)

    FileOutputStream(tempFile).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    getFileProviderUri(tempFile)
}

/**
 * 批量删除本地文件
 */
suspend fun deleteLocalFiles(paths: List<String>) = withContext(Dispatchers.IO) {
    paths.map { path ->
        async {
            deleteLocalFile(path)
        }
    }.awaitAll()
}

/**
 * 删除单个本地文件
 */
suspend fun deleteLocalFile(path: String?) = withContext(Dispatchers.IO) {
    deleteFile(path)
}

private fun deleteFile(path: String?) {
    path?.let {
        File(it).takeIf { file ->
            file.exists()
        }?.delete()
    }
}