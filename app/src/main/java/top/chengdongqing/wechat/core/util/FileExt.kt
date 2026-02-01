package top.chengdongqing.wechat.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.model.MediaResource
import java.io.File
import java.io.FileOutputStream

/**
 * 媒体文件预处理
 * 返回文件元数据
 */
suspend fun prepareMediaResource(context: Context, uri: Uri): MediaResource? =
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"

            val meta = queryMediaMetadata(context, uri)
            val fileName = meta.name ?: "FILE_${System.currentTimeMillis()}"

            val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
            val targetFile = File(mediaDir, fileName)
            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            var width = meta.width
            var height = meta.height
            var duration = meta.duration

            // 兜底策略
            val isImage = mimeType.startsWith("image/")
            val isVideo = mimeType.startsWith("video/")

            // 图片兜底解析
            if (isImage && width <= 0) {
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(targetFile.absolutePath, options)
                    width = options.outWidth
                    height = options.outHeight
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 视频兜底解析
            if (isVideo && (width <= 0 || duration <= 0)) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(targetFile.absolutePath)

                    if (width <= 0) {
                        width =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                ?.toIntOrNull() ?: 0
                    }
                    if (height <= 0) {
                        height =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                ?.toIntOrNull() ?: 0
                    }
                    if (duration <= 0) {
                        duration =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull() ?: 0
                    }

                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            MediaResource(
                file = targetFile,
                filename = fileName,
                mimeType = mimeType,
                size = targetFile.length(),
                width = width,
                height = height,
                duration = duration
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

/**
 * 核心查询逻辑：合并所有字段查询
 */
private data class TempMeta(val name: String?, val width: Int, val height: Int, val duration: Long)

private fun queryMediaMetadata(context: Context, uri: Uri): TempMeta {
    if (uri.scheme == "file") {
        return TempMeta(uri.lastPathSegment, 0, 0, 0)
    }

    val projection = arrayOf(
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.MediaColumns.DURATION
    )

    var name: String? = null
    var width = 0
    var height = 0
    var duration = 0L

    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val wIdx = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val hIdx = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val dIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

                if (nIdx != -1) name = cursor.getString(nIdx)
                if (wIdx != -1) width = cursor.getInt(wIdx)
                if (hIdx != -1) height = cursor.getInt(hIdx)
                if (dIdx != -1) duration = cursor.getLong(dIdx)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return TempMeta(name, width, height, duration)
}

/**
 * 分享文件
 */
fun Context.shareContent(content: Any, mimeType: String, title: String = "分享文件") {
    val shareUri: Uri = when (content) {
        is File -> getFileProviderUri(content)
        is Uri -> content
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
suspend fun Context.createMediaUri(isExtensionsVideo: Boolean): Uri = withContext(Dispatchers.IO) {
    val directory = if (isExtensionsVideo) "videos" else "images"
    val extension = if (isExtensionsVideo) ".mp4" else ".jpg"
    val prefix = if (isExtensionsVideo) "VID" else "IMG"

    val file = File(
        externalCacheDir,
        "$directory/${prefix}_${System.currentTimeMillis()}$extension"
    ).apply {
        parentFile?.mkdirs()
    }
    FileProvider.getUriForFile(this@createMediaUri, "${packageName}.provider", file)
}