package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.runtime.Composable
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.message.types.TextContent

@Composable
fun MessageContent(content: MessageContent) {
    when (content) {
        is MessageContent.Text -> {
            TextContent(content)
        }

        is MessageContent.Image -> {

        }

        is MessageContent.Voice -> {

        }

        else -> {}
    }
}