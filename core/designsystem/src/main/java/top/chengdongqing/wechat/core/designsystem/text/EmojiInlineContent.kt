package top.chengdongqing.wechat.core.designsystem.text

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

private const val InlineContentTag = "androidx.compose.foundation.text.inlineContent"

@Composable
fun rememberEmojiInlineContent(
    annotatedString: AnnotatedString,
    emojiSize: TextUnit,
): Map<String, InlineTextContent> {
    val context = LocalContext.current
    val density = LocalDensity.current
    val emojiSizePx = remember(emojiSize, density) { with(density) { emojiSize.toPx().toInt() } }
    val emojiSizeDp = remember(emojiSize, density) { with(density) { emojiSize.toDp() } }
    val descriptions = remember(annotatedString) {
        annotatedString
            .getStringAnnotations(InlineContentTag, 0, annotatedString.length)
            .map { it.item }
            .distinct()
    }
    val bitmaps = remember(descriptions, emojiSizePx) {
        descriptions.mapNotNull { description ->
            Emojis.findByDescription(description)?.let {
                description to it.toBitmap(context, emojiSizePx)
            }
        }.toMap()
    }

    return remember(bitmaps, emojiSize) {
        bitmaps.mapValues { (description, bitmap) ->
            InlineTextContent(
                Placeholder(emojiSize, emojiSize, PlaceholderVerticalAlign.TextCenter)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = description,
                    modifier = Modifier.size(emojiSizeDp)
                )
            }
        }
    }
}
