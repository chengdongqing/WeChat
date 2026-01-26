package top.chengdongqing.wechat.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.core.graphics.scale
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.data.sticker.Emojis

/**
 * 表情解析工具
 */
object EmojiManager {
    private val bitmapCache = LruCache<String, Bitmap>(50)

    /**
     * 获取表情 Bitmap
     */
    fun getEmojiBitmap(context: Context, emoji: Emoji, targetSize: Int): Bitmap {
        val cacheKey = "${emoji.localPath}_$targetSize"
        return bitmapCache.get(cacheKey) ?: run {
            context.assets.open(emoji.localPath).use { stream ->
                val raw = BitmapFactory.decodeStream(stream)
                raw.scale(targetSize, targetSize).also {
                    if (it != raw) raw.recycle()
                    bitmapCache.put(cacheKey, it)
                }
            }
        }
    }

    /**
     * 找出所有匹配项及其对应的表情对象
     */
    fun findAllMatches(text: CharSequence): List<EmojiMatch> {
        return EMOJI_PATTERN_REGEX.findAll(text).mapNotNull { match ->
            val desc = match.groupValues[1]
            val emoji = Emojis.find { it.description == desc }
            if (emoji != null) EmojiMatch(emoji, match.range) else null
        }.toList()
    }
}

data class EmojiMatch(val emoji: Emoji, val range: IntRange)

val EmojiMap: Map<String, Emoji> by lazy { Emojis.associateBy { it.description } }