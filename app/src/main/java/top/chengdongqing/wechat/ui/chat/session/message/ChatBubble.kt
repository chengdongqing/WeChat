package top.chengdongqing.wechat.ui.chat.session.message

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.sqrt

@Composable
fun ChatBubble(
    isFromMe: Boolean,
    showArrow: Boolean,
    content: @Composable () -> Unit
) {
    val bubbleColor = if (isFromMe) Color(0xFF95EC69) else Color.White
    val maxBubbleWidth = rememberMaxBubbleWidth()

    Surface(
        color = if (showArrow) bubbleColor else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .then(if (showArrow) Modifier.drawChatArrow(isFromMe, bubbleColor) else Modifier)
    ) {
        content()
    }
}

@Composable
private fun rememberMaxBubbleWidth(): Dp {
    val windowInfo = LocalWindowInfo.current
    val screenWidth = windowInfo.containerDpSize.width

    return remember {
        screenWidth - 60.dp - 40.dp - 24.dp
    }
}

/**
 * 绘制气泡箭头
 */
private fun Modifier.drawChatArrow(
    isFromMe: Boolean,
    color: Color,
    arrowSize: Dp = 8.dp,
    verticalOffset: Dp = 16.dp // 距离顶部的距离
): Modifier = this.drawWithCache {
    // 这里的逻辑只在 Size 改变时执行一次
    val sizePx = arrowSize.toPx()
    val offsetPx = verticalOffset.toPx()
    // 旋转 45 度后，顶点到中心的距离是 (边长 * √2) / 2
    val halfDiagonal = (sizePx * sqrt(2.0) / 2.0).toFloat()
    // 计算旋转中心
    val pivotX = if (isFromMe) size.width - 1 else 1f
    val pivotY = offsetPx + halfDiagonal
    // 计算正方形的左上角位置，使其中心点与 pivot 对齐
    val topLeft = Offset(
        x = pivotX - sizePx / 2f,
        y = pivotY - sizePx / 2f
    )

    onDrawBehind {
        // 这里的逻辑在重绘时执行
        rotate(
            degrees = 45f,
            pivot = Offset(pivotX, pivotY)
        ) {
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}