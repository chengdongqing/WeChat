package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.message.types.CallContent
import top.chengdongqing.wechat.ui.chat.session.message.types.ImageContent
import top.chengdongqing.wechat.ui.chat.session.message.types.LocationContent
import top.chengdongqing.wechat.ui.chat.session.message.types.StickerContent
import top.chengdongqing.wechat.ui.chat.session.message.types.TextContent
import top.chengdongqing.wechat.ui.chat.session.message.types.VideoContent
import top.chengdongqing.wechat.ui.chat.session.message.types.VoiceContent

@Composable
fun MessageContent(message: ChatMessage) {
    when (val content = message.content) {
        is MessageContent.Text -> TextContent(content)
        is MessageContent.Sticker -> StickerContent(content)
        is MessageContent.Image -> ImageContent(content)
        is MessageContent.Video -> VideoContent(content)
        is MessageContent.Voice -> VoiceContent(content)
        is MessageContent.Call -> CallContent(content, message.isFromMe)
        is MessageContent.Location -> LocationContent(content)
        else -> Unit
    }
}