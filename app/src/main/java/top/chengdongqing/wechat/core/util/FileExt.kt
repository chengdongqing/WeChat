package top.chengdongqing.wechat.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 分享文件
 */
fun Context.shareContent(content: Any, mimeType: String, title: String = "分享文件") {
    val shareUri: Uri = when (content) {
        is File -> getFileProviderUri(content)
        is Uri -> {
            when {
                content.scheme == "file" -> getFileProviderUri(File(content.path!!))
                else -> content
            }
        }

        else -> throw IllegalArgumentException("不支持的内容类型: ${content::class.java}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, shareUri)
        // 授予临时访问权限
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, title))
}

/**
 * 获取隔离的文件uri
 */
fun Context.getFileProviderUri(file: File): Uri {
    return FileProvider.getUriForFile(this, "$packageName.provider", file)
}

/**
 * 打开文件
 */
fun Context.openFile(file: File, mimeType: String, showChooser: Boolean = true) {
    val uri = getFileProviderUri(file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val finalIntent = if (showChooser) {
        Intent.createChooser(intent, "打开文件").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        intent
    }

    startActivity(finalIntent)
}

val String.asAssetPath: String
    get() = "file:///android_asset/$this"

suspend fun Context.createImageUri(bitmap: Bitmap, quality: Int = 100): Uri =
    withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("IMG_", ".jpg")

        FileOutputStream(tempFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        getFileProviderUri(tempFile)
    }

/**
 * 创建媒体文件记录
 */
suspend fun Context.createMediaUri(isVideo: Boolean = false): Uri = withContext(Dispatchers.IO) {
    val directory = if (isVideo) "videos" else "images"
    val extension = if (isVideo) ".mp4" else ".jpg"
    val prefix = if (isVideo) "VID" else "IMG"

    val file = File(
        externalCacheDir,
        "$directory/${prefix}_${System.currentTimeMillis()}$extension"
    ).apply {
        parentFile?.mkdirs()
    }
    FileProvider.getUriForFile(this@createMediaUri, "${packageName}.provider", file)
}

/**
 * 从文件名或路径中提取扩展名
 *
 * @return 扩展名（不含点），如 "jpg", "mp4"，未找到返回 null
 */
fun String?.extractFileExtension(): String? {
    if (isNullOrBlank()) return null
    val lastDotIndex = lastIndexOf('.')
    if (lastDotIndex == -1 || lastDotIndex == length - 1) {
        return null
    }
    return substring(lastDotIndex + 1)
}

/**
 * 将 Asset 文件复制到缓存目录并返回 File 对象
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
 * 文件元数据模型
 */
data class FileMetadata(
    val uri: Uri,
    val filename: String,
    val size: Long,
    val mimeType: String,
    // 以下为媒体特有属性
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,
    val isMedia: Boolean = false
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/**
 * 文件的元数据查询
 */
suspend fun Context.getFileMetadata(uri: Uri): FileMetadata? = withContext(Dispatchers.IO) {
    try {
        val resolver = contentResolver
        var name = "FILE_${System.currentTimeMillis()}"
        var size = 0L
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        // 基础查询
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nIdx != -1) name = cursor.getString(nIdx) ?: name
                if (sIdx != -1) size = cursor.getLong(sIdx)
            }
        }

        // 媒体属性探测
        var width = 0
        var height = 0
        var duration = 0L
        val isImage = mimeType.startsWith("image/")
        val isVideo = mimeType.startsWith("video/")

        if (isImage) {
            resolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                // 修正旋转后的宽高
                val (w, h) = getCorrectedDimensions(uri, options.outWidth, options.outHeight)
                width = w; height = h
            }
        } else if (isVideo) {
            MediaMetadataRetriever().apply {
                try {
                    setDataSource(this@getFileMetadata, uri)
                    val rotation =
                        extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                            ?: 0
                    val rawW =
                        extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                            ?: 0
                    val rawH =
                        extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                            ?: 0
                    duration =
                        extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                            ?: 0

                    if (rotation == 90 || rotation == 270) {
                        width = rawH; height = rawW
                    } else {
                        width = rawW; height = rawH
                    }
                } finally {
                    release()
                }
            }
        }

        FileMetadata(
            uri = uri,
            filename = name,
            size = size,
            mimeType = mimeType,
            width = width,
            height = height,
            duration = duration,
            isMedia = isImage || isVideo
        )
    } catch (e: Exception) {
        Log.e("FileExt", "getFileMetadata: [$uri]", e)
        null
    }
}

/**
 * 获取旋转校正后的尺寸
 */
private fun Context.getCorrectedDimensions(uri: Uri, w: Int, h: Int): Pair<Int, Int> {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                h to w
            } else {
                w to h
            }
        } ?: (w to h)
    } catch (_: Exception) {
        w to h
    }
}