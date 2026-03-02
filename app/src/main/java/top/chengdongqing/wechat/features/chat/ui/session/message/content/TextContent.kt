package top.chengdongqing.wechat.features.chat.ui.session.message.content

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.designsystem.model.Emojis
import top.chengdongqing.wechat.core.designsystem.util.parseRichText
import top.chengdongqing.wechat.core.designsystem.util.toBitmap
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

/**
 * 文本消息内容
 *
 * 支持富文本（URL、电话、表情）和自定义文本选择。
 */
@Composable
fun TextContent(message: ChatMessage) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val chatContext = LocalChatSessionContext.current
    val content = message.content as MessageContent.Text

    val emojiSize = 22.sp
    val emojiSizePx = with(density) { emojiSize.toPx().toInt() }
    val emojiSizeDp = with(density) { emojiSize.toDp() }

    /* 解析富文本（URL、电话高亮+点击） */
    val annotatedString = remember(content.text) {
        content.text.parseRichText(
            onUrlClick = { url -> chatContext?.onNavigateToWebView(url) },
            onPhoneClick = { phone ->
                val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                context.startActivity(intent)
            }
        )
    }

    /* 提取所有表情描述 */
    val emojiDescriptions = remember(annotatedString) {
        annotatedString.getStringAnnotations(
            "androidx.compose.foundation.text.inlineContent",
            0,
            annotatedString.length
        ).map { it.item }
    }

    /* 预加载表情 Bitmap */
    val emojiBitmaps = remember(emojiDescriptions, emojiSizePx) {
        emojiDescriptions.mapNotNull { description ->
            Emojis.findByDescription(description)?.let { emoji ->
                description to emoji.toBitmap(context, emojiSizePx)
            }
        }.toMap()
    }

    /* 创建 InlineContent */
    val inlineContent = remember(emojiBitmaps, emojiSize) {
        emojiBitmaps.mapValues { (description, bitmap) ->
            InlineTextContent(
                Placeholder(
                    width = emojiSize,
                    height = emojiSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = description,
                    modifier = Modifier.size(emojiSizeDp)
                )
            }
        }
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        modifier = Modifier.padding(10.dp),
        style = TextStyle(
            fontSize = 16.sp,
            color = Color.Black,
            lineHeight = 22.sp
        )
    )
}