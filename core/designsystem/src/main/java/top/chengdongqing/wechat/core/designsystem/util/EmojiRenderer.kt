package top.chengdongqing.wechat.core.designsystem.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.core.graphics.scale
import top.chengdongqing.wechat.core.designsystem.model.Emoji

object EmojiRenderer {
    // 根据可用内存动态调整缓存大小
    private val maxMemory = Runtime.getRuntime().maxMemory()
    private val cacheSize = (maxMemory / 8).toInt() // 使用 1/8 内存

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount  // 按实际内存计算，而非数量
        }
    }

    /**
     * 获取表情 Bitmap（带缓存）
     */
    fun getBitmap(
        context: Context,
        emoji: Emoji,
        targetSize: Int
    ): Bitmap {
        val cacheKey = "${emoji.localPath}_$targetSize"
        return bitmapCache.get(cacheKey) ?: loadAndCache(context, emoji, targetSize, cacheKey)
    }

    private fun loadAndCache(
        context: Context,
        emoji: Emoji,
        targetSize: Int,
        cacheKey: String
    ): Bitmap {
        return context.assets.open(emoji.localPath).use { stream ->
            val raw = BitmapFactory.decodeStream(stream)
            raw.scale(targetSize, targetSize).also {
                if (it != raw) raw.recycle()
                bitmapCache.put(cacheKey, it)
            }
        }
    }
}

fun Emoji.toBitmap(context: Context, size: Int): Bitmap {
    return EmojiRenderer.getBitmap(context, this, size)
}