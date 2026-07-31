package top.chengdongqing.wechat.core.common.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 文件元数据
 */
data class FileMetadata(
    val uri: Uri,
    val filename: String,
    val size: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,
    val isMedia: Boolean = false
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/**
 * 获取文件元数据
 *
 * 包含文件名、大小、MIME类型
 * 对于图片/视频还包含宽高、时长等
 */
suspend fun Context.getFileMetadata(uri: Uri): FileMetadata? =
    withContext(Dispatchers.IO) {
        try {
            val resolver = contentResolver
            var name = "FILE_${System.currentTimeMillis()}"
            var size = 0L
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"

            /**
             * 基础信息查询
             */
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }

            /**
             * 媒体属性探测
             */
            var width = 0
            var height = 0
            var duration = 0L
            val isImage = mimeType.startsWith("image/")
            val isVideo = mimeType.startsWith("video/")

            if (isImage) {
                resolver.openInputStream(uri)?.use { input ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(input, null, options)

                    /**
                     * 修正旋转后的宽高
                     */
                    val (w, h) = getCorrectedDimensions(uri, options.outWidth, options.outHeight)
                    width = w
                    height = h
                }
            } else if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    try {
                        retriever.setDataSource(this@getFileMetadata, uri)

                        val rotation = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                        )?.toIntOrNull() ?: 0

                        val rawW = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )?.toIntOrNull() ?: 0

                        val rawH = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )?.toIntOrNull() ?: 0

                        duration = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0

                        /**
                         * 根据旋转角度调整宽高
                         */
                        if (rotation == 90 || rotation == 270) {
                            width = rawH
                            height = rawW
                        } else {
                            width = rawW
                            height = rawH
                        }
                    } catch (e: Exception) {
                        Log.e("FileMetadata", "提取视频元数据失败", e)
                    }
                } finally {
                    retriever.release()
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
            Log.e("FileMetadata", "获取文件元数据失败: $uri", e)
            null
        }
    }

/**
 * 获取旋转校正后的尺寸
 *
 * 根据 EXIF 信息调整图片宽高
 */
private fun Context.getCorrectedDimensions(
    uri: Uri,
    width: Int,
    height: Int
): Pair<Int, Int> {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270
            ) {
                height to width
            } else {
                width to height
            }
        } ?: (width to height)
    } catch (_: Exception) {
        width to height
    }
}

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
    // 低版本直接返回原图 Uri
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
fun Context.loadVideoThumbnail(uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this, uri)
        retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}
