package top.chengdongqing.wechat.ui.chat.session.message.types

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.utils.EmojiManager
import top.chengdongqing.wechat.ui.utils.EmojiMap
import top.chengdongqing.wechat.ui.utils.parseRichText

@Composable
fun TextContent(content: MessageContent.Text) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current

    // 预计算表情大小
    val emojiSizePx = remember(density) { with(density) { 22.dp.roundToPx() } }

    // 解析富文本
    val annotatedString = remember(content.text) {
        parseRichText(
            content.text,
            onUrlClick = { url ->
                // 使用系统浏览器打开网址
                uriHandler.openUri(url)
            },
            onPhoneClick = { phone ->
                // 跳转到系统拨号盘
                val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                context.startActivity(intent)
            }
        )
    }

    // 表情占位替换为图片
    val inlineContent = remember(annotatedString) {
        annotatedString.getStringAnnotations(
            "androidx.compose.foundation.text.inlineContent",
            0,
            annotatedString.length
        )
            .map { it.item to EmojiMap[it.item] }
            .associate { (name, emoji) ->
                name to InlineTextContent(
                    Placeholder(22.sp, 22.sp, PlaceholderVerticalAlign.TextCenter)
                ) {
                    val bitmap = EmojiManager.getEmojiBitmap(context, emoji!!, emojiSizePx)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = name,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
    }

    SelectionContainer {
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
}