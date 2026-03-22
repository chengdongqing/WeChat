package top.chengdongqing.wechat.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.AnyRes
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 将 Assets 文件复制到指定文件
 */
suspend fun Context.copyAssetToUri(
    assetName: String,
    targetFile: File
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        assets.open(assetName).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        getFileProviderUri(targetFile)
    }.getOrNull()
}

/**
 * 将 Resource 资源文件复制到指定文件
 */
suspend fun Context.copyResourceToUri(
    @AnyRes resId: Int,
    targetFile: File
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        resources.openRawResource(resId).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        getFileProviderUri(targetFile)
    }.getOrNull()
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