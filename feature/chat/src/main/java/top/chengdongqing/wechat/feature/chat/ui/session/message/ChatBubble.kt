package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.chat.WeMessageBubble
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme

/**
 * 聊天气泡组件
 */
@Composable
fun ChatBubble(
    isFromMe: Boolean,
    isPressed: Boolean,
    showArrow: Boolean,
    showDot: Boolean,
    isSelectMode: Boolean,
    isFailed: Boolean,
    isSameBackground: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = ChatTheme.colorScheme
    val bubbleColor = if (isFromMe && !isSameBackground) {
        colors.bubbleOutgoing
    } else {
        colors.bubbleIncoming
    }
    val maxBubbleWidth = rememberMaxBubbleWidth(isSelectMode, isFailed)

    WeBadge(
        visible = showDot,
        size = 8.dp,
        alignment = if (isFromMe) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        WeMessageBubble(
            isFromMe = isFromMe,
            color = bubbleColor,
            modifier = modifier,
            isPressed = isPressed,
            maxWidth = maxBubbleWidth,
            showArrow = showArrow
        ) {
            content()
        }
    }
}

/**
 * 计算气泡最大宽度
 */
@Composable
private fun rememberMaxBubbleWidth(isSelectMode: Boolean, isFailed: Boolean): Dp {
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerDpSize.width

    return remember(isSelectMode, isFailed) {
        val width = screenWidth - 60.dp - 40.dp - 24.dp
        if (isSelectMode && isFailed) {
            width - 22.dp
        } else {
            width
        }
    }
}
