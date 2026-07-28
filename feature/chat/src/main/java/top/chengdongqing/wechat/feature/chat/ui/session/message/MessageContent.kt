package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.CallContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.ContactCardContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.FileContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.LiveContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.LocationContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.MediaContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.MusicContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.StickerContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.TextContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.content.VoiceContent

/**
 * 消息内容渲染
 */
@Composable
fun MessageContent(message: ChatMessage) {
    when (val content = message.content) {
        is MessageContent.Text -> TextContent(message)
        is MessageContent.Voice -> VoiceContent(message)
        is MessageContent.Sticker -> StickerContent(content)
        is MessageContent.Image, is MessageContent.Video -> MediaContent(message)
        is MessageContent.Call -> CallContent(message)
        is MessageContent.Location -> LocationContent(content)
        is MessageContent.File -> FileContent(message)
        is MessageContent.ContactCard -> ContactCardContent(content)
        is MessageContent.Music -> MusicContent(content)
        is MessageContent.Live -> LiveContent(message)
        else -> Unit
    }
}
