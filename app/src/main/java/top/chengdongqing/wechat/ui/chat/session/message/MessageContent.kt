package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.message.types.ImageContent
import top.chengdongqing.wechat.ui.chat.session.message.types.StickerContent
import top.chengdongqing.wechat.ui.chat.session.message.types.TextContent
import top.chengdongqing.wechat.ui.chat.session.message.types.VideoContent

@Composable
fun MessageContent(content: MessageContent) {
    when (content) {
        is MessageContent.Text -> TextContent(content)
        is MessageContent.Sticker -> StickerContent(content)
        is MessageContent.Image -> ImageContent(content)
        is MessageContent.Video -> VideoContent(content)
        is MessageContent.Voice -> {}
        else -> Unit
    }
}