package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.CallContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.FileContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.LocationContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.MediaContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.StickerContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.TextContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.UserCardContent
import top.chengdongqing.wechat.features.chat.ui.session.message.content.VoiceContent

/**
 * 消息内容渲染
 */
@Composable
fun MessageContent(
    message: ChatMessage,
    isTextSelectable: Boolean = false,
    textSelection: TextRange? = null,
    onTextSelectionChange: (TextRange) -> Unit = {},
    onTextSelectionDismiss: () -> Unit = {}
) {
    when (val content = message.content) {
        is MessageContent.Text -> TextContent(
            message = message,
            isSelectable = isTextSelectable,
            selection = textSelection,
            onSelectionChange = onTextSelectionChange,
            onSelectionDismiss = onTextSelectionDismiss
        )

        is MessageContent.Voice -> VoiceContent(message)
        is MessageContent.Sticker -> StickerContent(content)
        is MessageContent.Image, is MessageContent.Video -> MediaContent(message)
        is MessageContent.Call -> CallContent(message)
        is MessageContent.Location -> LocationContent(content)
        is MessageContent.File -> FileContent(message)
        is MessageContent.ContactCard -> UserCardContent(content)
        else -> Unit
    }
}