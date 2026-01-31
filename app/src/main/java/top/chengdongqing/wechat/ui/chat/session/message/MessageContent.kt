package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.message.content.CallContent
import top.chengdongqing.wechat.ui.chat.session.message.content.FileContent
import top.chengdongqing.wechat.ui.chat.session.message.content.LocationContent
import top.chengdongqing.wechat.ui.chat.session.message.content.MediaContent
import top.chengdongqing.wechat.ui.chat.session.message.content.StickerContent
import top.chengdongqing.wechat.ui.chat.session.message.content.TextContent
import top.chengdongqing.wechat.ui.chat.session.message.content.UserCardContent
import top.chengdongqing.wechat.ui.chat.session.message.content.VoiceContent

@Composable
fun MessageContent(message: ChatMessage) {
    when (val content = message.content) {
        is MessageContent.Text -> TextContent(content)
        is MessageContent.Voice -> VoiceContent(message)
        is MessageContent.Sticker -> StickerContent(content)
        is MessageContent.Image, is MessageContent.Video -> MediaContent(content)
        is MessageContent.Call -> CallContent(message)
        is MessageContent.Location -> LocationContent(content)
        is MessageContent.File -> FileContent(content)
        is MessageContent.UserCard -> UserCardContent(content)
        else -> Unit
    }
}