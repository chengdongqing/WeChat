package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.common.util.isWithinSeconds
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.ui.messageRes
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

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
    val resources = LocalResources.current
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
                            append(resources.getString(R.string.chat_recalled_by_me))
                        } else {
                            append(resources.getString(R.string.chat_recalled_by_other))
                        }
                    }

                    message.isSent -> append(resources.getString(R.string.chat_sent_no_receipt))

                    error != null -> append(resources.getString(error.messageRes))
                }
            }

            // 根据错误类型添加可点击链接
            val (actionLabel, actionAnnotation) = when {
                !message.isRecalled && message.isFromMe && (error?.canRetry == true || message.isSent) -> {
                    val label = if (error == SendError.Cancelled) {
                        resources.getString(R.string.chat_action_resend)
                    } else {
                        resources.getString(R.string.chat_action_retry)
                    }
                    label to LinkAnnotation.Clickable(
                        tag = "retry",
                        styles = linkStyles,
                        linkInteractionListener = { chatContext?.onRetrySend(message.id) }
                    )
                }

                !message.isRecalled && error == SendError.NotFriend -> {
                    resources.getString(R.string.chat_action_send_verify) to LinkAnnotation.Clickable(
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
                    resources.getString(R.string.chat_action_reedit) to LinkAnnotation.Clickable(
                        tag = "reedit",
                        styles = linkStyles,
                        linkInteractionListener = { chatContext?.onReeditMessage((message.content as MessageContent.Text).text) }
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