package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.data.database.entity.SendError
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

/**
 * 失败消息提示
 */
@Composable
fun FailedMessageHint(message: ChatMessage) {
    val hintText = rememberHintText(message)

    Text(
        text = hintText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
}

@Composable
private fun rememberHintText(message: ChatMessage): AnnotatedString {
    val chatContext = LocalChatSessionContext.current
    val textColor = WeTheme.colorScheme.textSecondary
    val linkStyles = rememberLinkStyles()
    val error = message.error

    return remember(message) {
        buildAnnotatedString {
            // 绘制主体提示文字
            withStyle(style = SpanStyle(color = textColor, fontSize = 13.sp)) {
                when {
                    message.isRecalled -> {
                        if (message.isFromMe) {
                            append("你撤回了一条消息")
                        } else {
                            append("对方撤回了一条消息")
                        }
                    }

                    message.isSent -> append("消息已发出，但未收到对方的回执。")

                    error != null -> append(error.message)
                }
            }

            // 根据错误类型添加可点击链接
            val (actionLabel, actionAnnotation) = when {
                !message.isRecalled && (error?.canRetry.isTrue()
                        || message.isSent) -> {
                    val label = if (error == SendError.Cancelled) "再次发送" else "重试"
                    label to LinkAnnotation.Clickable(
                        tag = "retry",
                        styles = linkStyles,
                        linkInteractionListener = { chatContext?.onRetrySend(message.id) }
                    )
                }

                !message.isRecalled && error == SendError.NotFriend -> {
                    "发送朋友验证" to LinkAnnotation.Clickable(
                        tag = "verify",
                        styles = linkStyles,
                        linkInteractionListener = { chatContext?.onNavigateToRequestAddFriend() }
                    )
                }

                /**
                 * 已撤回的消息可以重新编辑，需满足：
                 * 1. 是我发的
                 * 2. 文本消息
                 * 3. 5分钟内发的
                 */
                message.isRecalled && message.isFromMe
                        && message.content is MessageContent.Text
                        && message.timestamp.isWithinSeconds() -> {
                    "重新编辑" to LinkAnnotation.Clickable(
                        tag = "reedit",
                        styles = linkStyles,
                        linkInteractionListener = { chatContext?.onReeditMessage(message.content.text) }
                    )
                }

                else -> null to null
            }

            if (actionLabel != null && actionAnnotation != null) {
                append(" ")
                val start = length
                append(actionLabel)
                addLink(
                    clickable = actionAnnotation,
                    start = start,
                    end = length
                )
            }
        }
    }
}

@Composable
private fun rememberLinkStyles(): TextLinkStyles {
    val linkColor = WeTheme.colorScheme.link

    return remember {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                fontSize = 13.sp
            ),
            pressedStyle = SpanStyle(
                color = linkColor.copy(alpha = 0.7f)
            )
        )
    }
}