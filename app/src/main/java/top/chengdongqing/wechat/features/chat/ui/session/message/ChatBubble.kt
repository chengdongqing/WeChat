package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.features.chat.theme.ChatTheme
import kotlin.math.sqrt

/**
 * 聊天气泡组件
 */
@Composable
fun ChatBubble(
    isFromMe: Boolean,
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
        Surface(
            color = if (showArrow) bubbleColor else Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
                .widthIn(max = maxBubbleWidth)
                .then(if (showArrow) Modifier.drawBubbleArrow(isFromMe, bubbleColor) else Modifier)
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

/**
 * 绘制气泡箭头
 */
private fun Modifier.drawBubbleArrow(
    isFromMe: Boolean,
    color: Color,
    arrowSize: Dp = 8.dp,
    verticalOffset: Dp = 16.dp
): Modifier = this.drawWithCache {
    val sizePx = arrowSize.toPx()
    val offsetPx = verticalOffset.toPx()
    // 旋转 45 度后，顶点到中心的距离是 (边长 * √2) / 2
    val halfDiagonal = (sizePx * sqrt(2.0) / 2.0).toFloat()

    // 计算旋转中心
    val pivotX = if (isFromMe) size.width - 1.5f else 1f
    val pivotY = offsetPx + halfDiagonal

    // 计算正方形的左上角位置
    val topLeft = Offset(
        x = pivotX - sizePx / 2f,
        y = pivotY - sizePx / 2f
    )

    onDrawBehind {
        rotate(
            degrees = 45f,
            pivot = Offset(pivotX, pivotY)
        ) {
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
        }
    }
}