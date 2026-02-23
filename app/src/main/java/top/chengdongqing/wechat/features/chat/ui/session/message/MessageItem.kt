package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

@Composable
fun MessageItem(
    message: ChatMessage,
    peerAvatar: String?,
    myAvatar: String?,
) {
    val isFromMe = message.isFromMe
    val content = message.content

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isFromMe) {
                    Avatar(localPath = peerAvatar, isPeer = true)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFromMe) {
                        StatusIndicator(message)
                    }
                    ChatBubble(
                        isFromMe = isFromMe,
                        showArrow = content.showBubbleArrow,
                        showDot = content.showUnreadDot && !isFromMe,
                        isSameBackground = content.isSameBackground
                    ) {
                        MessageContent(message)
                    }
                    if (!isFromMe) {
                        StatusIndicator(message)
                    }
                }

                if (isFromMe) {
                    Avatar(localPath = myAvatar, isPeer = false)
                }
            }
        }

        if (message.isFailed) {
            FailedMessageHint(message)
        }
    }
}

@Composable
private fun Avatar(localPath: String?, isPeer: Boolean) {
    val chatContext = LocalChatSessionContext.current

    AsyncImage(
        model = localPath,
        contentDescription = null,
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .weClickable {
                chatContext?.onNavigateToContact(isPeer)
            }
    )
}

/**
 * 发送状态指示器
 */
@Composable
private fun StatusIndicator(message: ChatMessage) {
    when {
        message.isSending -> {
            if (message.content.showLoading) {
                WeLoading()
            }
        }

        message.isFailed -> Image(
            painter = painterResource(R.drawable.ic_error_circle_filled),
            contentDescription = "错误",
            modifier = Modifier.size(24.dp)
        )
    }
}