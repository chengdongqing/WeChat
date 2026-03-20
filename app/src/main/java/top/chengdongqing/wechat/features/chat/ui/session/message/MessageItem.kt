package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

/**
 * 消息项组件
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    peerAvatar: Any? = null,
    myAvatar: Any? = null,
    isSelectMode: Boolean = false,
    isMessageSelected: Boolean = false,
    onMessageClick: () -> Unit = {},
    onMessageLongPress: (bubblePosition: Offset, bubbleHeight: Float) -> Unit = { _, _ -> }
) {
    val isFromMe = message.isFromMe
    val content = message.content

    /* 记录气泡在窗口中的位置和高度 */
    var bubblePosition by remember { mutableStateOf(Offset.Zero) }
    var bubbleHeight by remember { mutableFloatStateOf(0f) }

    Column {
        if (!message.isRecalled) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
                ) {
                    /**
                     * 复选框
                     */
                    if (isSelectMode) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .then(
                                    if (isFromMe) Modifier.weight(1f) else Modifier
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            WeCheckBox(checked = isMessageSelected)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        /**
                         * 头像
                         */
                        if (!isFromMe) {
                            Avatar(model = peerAvatar, isPeer = true)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            /**
                             * 发送状态指示器
                             */
                            if (isFromMe) {
                                StatusIndicator(message)
                            }

                            /**
                             * 气泡容器
                             */
                            ChatBubble(
                                isFromMe = isFromMe,
                                showArrow = content.showBubbleArrow,
                                showDot = content.showUnreadDot && !isFromMe,
                                isSelectMode = isSelectMode,
                                isFailed = message.isFailed,
                                isSameBackground = content.isSameBackground,
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        bubblePosition = coordinates.positionInWindow()
                                        bubbleHeight = coordinates.size.height.toFloat()
                                    }
                                    .combinedClickable(
                                        onClick = onMessageClick,
                                        onLongClick = {
                                            onMessageLongPress(bubblePosition, bubbleHeight)
                                        }
                                    )
                            ) {
                                /**
                                 * 消息内容
                                 */
                                MessageContent(message)
                            }

                            if (!isFromMe) {
                                StatusIndicator(message)
                            }
                        }

                        if (isFromMe) {
                            Avatar(model = myAvatar, isPeer = false)
                        }
                    }
                }

                /**
                 * 多选模式下方便点击的遮罩
                 */
                if (isSelectMode) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(1f)
                            .weClickable(onClick = onMessageClick)
                    )
                }
            }
        }

        /**
         * 发送失败/撤回 等情况下的提示信息
         */
        if (message.isFailed || message.isSent || message.isRecalled) {
            FailedMessageHint(message)
        }
    }
}

/**
 * 头像组件
 */
@Composable
private fun Avatar(model: Any?, isPeer: Boolean) {
    val chatContext = LocalChatSessionContext.current

    AsyncImage(
        model = model,
        contentDescription = null,
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .weClickable {
                chatContext?.onNavigateToContact(isPeer)
            })
}

/**
 * 发送状态指示器
 */
@Composable
private fun StatusIndicator(message: ChatMessage) {
    when {
        message.isProgressing -> {
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