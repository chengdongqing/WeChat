package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.data.model.ChatHistoryItem
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.text.RichTextMode
import top.chengdongqing.wechat.core.designsystem.text.parseRichText
import top.chengdongqing.wechat.core.designsystem.text.rememberEmojiInlineContent
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun ChatHistoryContent(content: MessageContent.ChatHistory) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .padding(12.dp)
    ) {
        Text(
            text = content.title,
            fontSize = 17.sp,
            color = WeTheme.colorScheme.textPrimary
        )
        Spacer(Modifier.height(7.dp))
        content.items.take(4).forEach { item ->
            ChatHistoryPreviewLine(item)
        }
        Spacer(Modifier.height(9.dp))
        WeDivider()
        Spacer(Modifier.height(7.dp))
        Text(
            text = "聊天记录",
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary
        )
    }
}

@Composable
private fun ChatHistoryPreviewLine(item: ChatHistoryItem) {
    val preview = remember(item.senderName, item.text) {
        "${item.senderName}: ${item.text}".parseRichText(mode = RichTextMode.EmojiOnly)
    }
    val inlineContent = rememberEmojiInlineContent(preview, emojiSize = 17.sp)

    Text(
        text = preview,
        inlineContent = inlineContent,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = WeTheme.colorScheme.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
