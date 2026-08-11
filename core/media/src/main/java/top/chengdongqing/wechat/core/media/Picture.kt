package top.chengdongqing.wechat.core.media

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 是否为横图
 */
suspend fun isLandscape(filePath: String): Boolean = withContext(Dispatchers.IO) {
    if (filePath.isBlank()) return@withContext false

    // 获取物理宽高
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(filePath, options)

    val width = options.outWidth
    val height = options.outHeight

    // 如果读取失败（文件损坏或不存在），直接返回 false
    if (width <= 0 || height <= 0) return@withContext false

    // 获取 EXIF 旋转信息
    val rotation = runCatching {
        val exif = ExifInterface(filePath)
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL) // 读取失败则认为没有旋转

    // 根据旋转角度，判断是否需要交换宽高
    val isSwapped = rotation == ExifInterface.ORIENTATION_ROTATE_90 ||
            rotation == ExifInterface.ORIENTATION_ROTATE_270

    val finalWidth = if (isSwapped) height else width
    val finalHeight = if (isSwapped) width else height

    // 最终比较
    finalWidth > finalHeight
}