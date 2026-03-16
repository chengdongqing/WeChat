package top.chengdongqing.wechat.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    // Android 10 以下的图片直接返回原图 Uri
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !isVideo) {
        return uri
    }

    return withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ 使用系统缩略图加载
                contentResolver.loadThumbnail(uri, size, null)
            } else {
                // API 29 以下手动提取视频首帧
                loadVideoThumbnail(uri)
            }
        }.getOrElse {
            if (!isVideo) uri else loadVideoThumbnail(uri)
        }
    }
}

/**
 * 提取视频首帧
 *
 * 用于低版本系统
 */
fun Context.loadVideoThumbnail(uri: Uri): Bitmap? = MediaMetadataRetriever().use { retriever ->
    runCatching {
        retriever.setDataSource(this, uri)
        retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }.getOrNull()
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
    runCatching {
        val dir = File(filesDir, subDir).also { it.mkdirs() }
        val fileName = "${randomUUID()}_${getFileName(uri)}"
        val destFile = File(dir, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, 64 * 1024)
            }
        } ?: throw IOException("文件打开失败")

        destFile.absolutePath
    }.getOrNull()
}

/**
 * 复制 Asset 文件到私有目录
 * 若已存在则不重新创建
 *
 * @param assetName Asset 文件名
 * @return 文件绝对路径，失败返回 null
 */
suspend fun Context.copyAssetToPrivateDir(
    assetName: String
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val destFile = File(filesDir, assetName).also {
            it.parentFile?.mkdirs()
        }
        if (destFile.exists()) {
            return@runCatching destFile.absolutePath
        }

        assets.open(assetName).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        destFile.absolutePath
    }.getOrNull()
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
    val tempFile = File.createTempFile("IMG_", ".jpg")

    FileOutputStream(tempFile).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    getFileProviderUri(tempFile)
}

/**
 * 生成头像缩略图
 *
 * 返回图片二进制数据
 */
fun File.toBytes(
    targetSize: Int = 80,
    maxSizeKB: Int = 5,
    quality: Int = 70
): ByteArray? = runCatching {
    if (!exists()) return null

    // 生成 Bitmap
    val bitmap = BitmapFactory.decodeFile(path) ?: return null

    // 缩放到目标尺寸
    val thumbnail = bitmap.scale(targetSize, targetSize)

    // 压缩到指定大小
    val outputStream = ByteArrayOutputStream()
    var currentQuality = quality
    do {
        outputStream.reset()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
        currentQuality -= 10
    } while (outputStream.size() > maxSizeKB * 1024 && currentQuality > 10)

    // 转 byte array
    outputStream.toByteArray().also {
        // 回收资源
        bitmap.recycle()
        thumbnail.recycle()
    }
}.getOrNull()