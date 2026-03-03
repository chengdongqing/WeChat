package top.chengdongqing.wechat.core.designsystem.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.TextUnit
import top.chengdongqing.wechat.core.designsystem.model.Emojis

private const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"

/**
 * 从已解析的 [AnnotatedString] 中提取表情占位符，加载 Bitmap，
 *
 * @param annotatedString 经 parseRichText 处理、含表情 annotation 的文本
 * @param emojiSize       渲染尺寸，应与 Text 的 fontSize 保持一致
 */
@Composable
fun rememberEmojiInlineContent(
    annotatedString: AnnotatedString,
    emojiSize: TextUnit,
): Map<String, InlineTextContent> {
    val context = LocalContext.current
    val density = LocalDensity.current

    val emojiSizePx = remember(emojiSize, density) { with(density) { emojiSize.toPx().toInt() } }
    val emojiSizeDp = remember(emojiSize, density) { with(density) { emojiSize.toDp() } }

    // 去重后再加载，避免同一表情重复解码 Bitmap
    val descriptions = remember(annotatedString) {
        annotatedString
            .getStringAnnotations(INLINE_CONTENT_TAG, 0, annotatedString.length)
            .map { it.item }
            .distinct()
    }

    val bitmaps = remember(descriptions, emojiSizePx) {
        descriptions.mapNotNull { desc ->
            Emojis.findByDescription(desc)?.let { desc to it.toBitmap(context, emojiSizePx) }
        }.toMap()
    }

    return remember(bitmaps, emojiSize) {
        bitmaps.mapValues { (desc, bitmap) ->
            InlineTextContent(
                Placeholder(emojiSize, emojiSize, PlaceholderVerticalAlign.TextCenter)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = desc,
                    modifier = Modifier.size(emojiSizeDp),
                )
            }
        }
    }
}