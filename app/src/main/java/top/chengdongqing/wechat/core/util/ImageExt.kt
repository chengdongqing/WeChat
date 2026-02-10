package top.chengdongqing.wechat.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageExt @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ImageUtils"
    }

    /**
     * 生成缩略图二进制
     *
     * @param imagePath 原图路径
     * @param targetSize 目标尺寸（正方形）
     * @param maxSizeKB 最大文件大小（KB）
     * @param quality 初始压缩质量
     * @return 压缩后的 JPEG 字节数组
     */
    fun generateThumbnailBytes(
        imagePath: String,
        targetSize: Int = 80,
        maxSizeKB: Int = 5,
        quality: Int = 70
    ): ByteArray? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                Log.w(TAG, "图片文件不存在: $imagePath")
                return null
            }

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.w(TAG, "无法解码图片: $imagePath")
                return null
            }

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

            val bytes = outputStream.toByteArray()

            bitmap.recycle()
            thumbnail.recycle()

            Log.d(TAG, "缩略图生成成功: ${bytes.size} 字节, 质量: $currentQuality")
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "生成缩略图失败", e)
            null
        }
    }

    /**
     * 生成完整头像二进制
     *
     * @param imagePath 原图路径
     * @param maxSize 最大边长
     * @param quality 压缩质量
     */
    fun generateFullAvatarBytes(
        imagePath: String,
        maxSize: Int = 300,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

            // 计算缩放比例（保持宽高比）
            val scale = minOf(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height,
                1.0f  // 不放大
            )

            val scaledBitmap = if (scale < 1.0f) {
                bitmap.scale(
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt()
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            val bytes = outputStream.toByteArray()

            bitmap.recycle()
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            Log.d(TAG, "完整头像生成: ${bytes.size} 字节")
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "生成完整头像失败", e)
            null
        }
    }

    /**
     * 保存字节数组为图片文件
     *
     * @param userId 用户ID
     * @param bytes 图片字节数组
     * @param isThumbnail 是否是缩略图
     * @return 保存的文件路径
     */
    fun saveAvatarBytes(
        userId: String,
        bytes: ByteArray,
        isThumbnail: Boolean = false
    ): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                Log.e(TAG, "无法解码图片字节数组")
                return null
            }

            val filename = if (isThumbnail) {
                "avatar_${userId}_thumb.jpg"
            } else {
                "avatar_$userId.jpg"
            }

            val file = File(context.filesDir, filename)
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }

            bitmap.recycle()

            Log.d(TAG, "头像已保存: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存头像失败", e)
            null
        }
    }

    /**
     * 删除用户头像
     */
    fun deleteAvatar(userId: String) {
        try {
            File(context.filesDir, "avatar_$userId.jpg").delete()
            File(context.filesDir, "avatar_${userId}_thumb.jpg").delete()
            Log.d(TAG, "头像已删除: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "删除头像失败", e)
        }
    }

    /**
     * 获取头像文件路径
     */
    fun getAvatarPath(userId: String, thumbnail: Boolean = false): String? {
        val filename = if (thumbnail) {
            "avatar_${userId}_thumb.jpg"
        } else {
            "avatar_$userId.jpg"
        }

        val file = File(context.filesDir, filename)
        return if (file.exists()) file.absolutePath else null
    }
}