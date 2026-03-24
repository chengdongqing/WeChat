package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.theme.scaled
import top.chengdongqing.wechat.core.designsystem.util.parseRichText
import top.chengdongqing.wechat.core.designsystem.util.rememberEmojiInlineContent
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

/**
 * 文本消息内容
 *
 * 支持富文本（URL、电话、表情）
 */
@Composable
fun TextContent(message: ChatMessage) {
    val context = LocalContext.current
    val chatContext = LocalChatSessionContext.current
    val content = message.content as MessageContent.Text

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

    val inlineContent = rememberEmojiInlineContent(annotatedString, emojiSize = 22.sp)
    val colors = ChatTheme.colorScheme

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        modifier = Modifier.padding(10.dp),
        style = TextStyle(
            fontSize = 16.sp.scaled,
            color = if (message.isFromMe) colors.bubbleTextOutgoing else colors.bubbleTextIncoming,
            lineHeight = 22.sp.scaled
        )
    )
}