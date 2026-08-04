package top.chengdongqing.wechat.core.designsystem.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.theme.scaled
import kotlin.math.sqrt

@Composable
fun WeMessageBubble(
    isFromMe: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    isPressed: Boolean = false,
    maxWidth: Dp = LocalWindowInfo.current.containerDpSize.width - 124.dp,
    showArrow: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        color = if (showArrow) color else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .widthIn(max = maxWidth)
            .then(
                if (showArrow) {
                    Modifier.drawMessageBubbleArrow(isFromMe, color, isPressed)
                } else {
                    Modifier
                }
            )
    ) {
        Box {
            content()
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Composable
fun WeMessageText(
    text: AnnotatedString,
    color: Color,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        inlineContent = inlineContent,
        modifier = modifier,
        style = TextStyle(
            fontSize = 16.sp.scaled,
            lineHeight = 22.sp.scaled,
            color = color
        ),
        onTextLayout = onTextLayout
    )
}

private fun Modifier.drawMessageBubbleArrow(
    isFromMe: Boolean,
    color: Color,
    isPressed: Boolean,
    arrowSize: Dp = 8.dp,
    verticalOffset: Dp = 16.dp
): Modifier = drawWithCache {
    val sizePx = arrowSize.toPx()
    val halfDiagonal = (sizePx * sqrt(2.0) / 2.0).toFloat()
    val pivotX = if (isFromMe) size.width - 1.5f else 1f
    val pivotY = verticalOffset.toPx() + halfDiagonal
    val topLeft = Offset(pivotX - sizePx / 2f, pivotY - sizePx / 2f)

    onDrawBehind {
        rotate(45f, Offset(pivotX, pivotY)) {
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(sizePx, sizePx),
                cornerRadius = CornerRadius(1.5.dp.toPx())
            )
            if (isPressed) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.1f),
                    topLeft = topLeft,
                    size = Size(sizePx, sizePx),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }
    }
}
