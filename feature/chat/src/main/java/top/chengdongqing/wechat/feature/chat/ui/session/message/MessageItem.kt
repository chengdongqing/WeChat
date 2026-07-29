package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.util.onTap
import top.chengdongqing.wechat.core.model.MessageSendStatus
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

/**
 * 消息项组件
 */
@Composable
fun MessageItem(
    message: ChatMessage,
    albumMessages: List<ChatMessage> = emptyList(),
    onAlbumMediaClick: (ChatMessage) -> Unit = {},
    peerAvatar: Any? = null,
    myAvatar: Any? = null,
    isSelectMode: Boolean = false,
    isMessageSelected: Boolean = false,
    shakeOffsetX: Float = 0f,
    shakeOffsetY: Float = 0f,
    shakeRotation: Float = 0f,
    shakeScale: Float = 1f,
    textSelection: TextRange? = null,
    onTextSelectionChange: (TextRange) -> Unit = {},
    onTextSelectionDragChange: (Boolean) -> Unit = {},
    onTextSelectionBoundsChange: (Offset, Float) -> Unit = { _, _ -> },
    quoteSenderName: String = "",
    onQuoteClick: (String) -> Unit = {},
    onMessageClick: () -> Unit = {},
    onMessageLongPress: (bubblePosition: Offset, bubbleHeight: Float) -> Unit = { _, _ -> }
) {
    val isFromMe = message.isFromMe
    val content = message.content

    /* 记录气泡在窗口中的位置和高度 */
    var bubblePosition by remember { mutableStateOf(Offset.Zero) }
    var bubbleHeight by remember { mutableFloatStateOf(0f) }
    var bubbleWidth by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.graphicsLayer {
            translationX = shakeOffsetX
            translationY = shakeOffsetY
            rotationZ = shakeRotation
            scaleX = shakeScale
            scaleY = shakeScale
        }
    ) {
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
                                        bubbleWidth = coordinates.size.width.toFloat()
                                        bubbleHeight = coordinates.size.height.toFloat()
                                    }
                                    .combinedClickable(
                                        onClick = onMessageClick,
                                        onLongClick = {
                                            onMessageLongPress(
                                                bubblePosition + Offset(bubbleWidth / 2f, 0f),
                                                bubbleHeight
                                            )
                                        }
                                    )
                            ) {
                                /**
                                 * 消息内容
                                 */
                                Column {
                                    message.quote?.let { quote ->
                                        Column(
                                            modifier = Modifier
                                                .clickable { onQuoteClick(quote.messageId) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = quoteSenderName,
                                                color = ChatTheme.colorScheme.timestamp,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = quote.preview,
                                                color = ChatTheme.colorScheme.timestamp,
                                                fontSize = 12.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    MessageContent(
                                        message = message,
                                        albumMessages = albumMessages,
                                        onAlbumMediaClick = onAlbumMediaClick,
                                        textSelection = textSelection,
                                        onTextSelectionChange = onTextSelectionChange,
                                        onTextSelectionDragChange = onTextSelectionDragChange,
                                        onTextSelectionBoundsChange = onTextSelectionBoundsChange
                                    )
                                }
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
                            .onTap(onClick = onMessageClick)
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
            .onTap {
                chatContext?.onNavigateToContact(isPeer)
            })
}

/**
 * 发送状态指示器
 */
@Composable
private fun StatusIndicator(message: ChatMessage) {
    when {
        message.sendStatus is MessageSendStatus.Sending -> {
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
