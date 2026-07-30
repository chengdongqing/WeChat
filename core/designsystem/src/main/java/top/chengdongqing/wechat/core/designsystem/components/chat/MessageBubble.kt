package top.chengdongqing.wechat.core.designsystem.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.Neutral900
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryDark
import top.chengdongqing.wechat.core.designsystem.theme.TextPrimaryLight
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.theme.scaled
import kotlin.math.sqrt

@Composable
fun WeMessageBubble(
    isFromMe: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    maxWidth: Dp = LocalWindowInfo.current.containerDpSize.width - 124.dp,
    showArrow: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        color = if (showArrow) color else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .widthIn(max = maxWidth)
            .then(if (showArrow) Modifier.drawMessageBubbleArrow(isFromMe, color) else Modifier)
    ) {
        content()
    }
}

@Composable
fun TextMessagePreviewItem(
    text: String,
    isFromMe: Boolean,
    myAvatar: Any?,
    peerAvatar: Any?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppearanceSetting.current.isDarkTheme
    val bubbleColor = when {
        isFromMe && isDark -> Color(0xFF3DAF72)
        isFromMe -> Color(0xFF95EC69)
        isDark -> Neutral900
        else -> White
    }
    val textColor = when {
        isFromMe -> TextPrimaryLight
        isDark -> TextPrimaryDark
        else -> TextPrimaryLight
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!isFromMe) MessagePreviewAvatar(peerAvatar)
            WeMessageBubble(isFromMe = isFromMe, color = bubbleColor) {
                WeMessageText(
                    text = AnnotatedString(text),
                    modifier = Modifier.padding(10.dp),
                    color = textColor
                )
            }
            if (isFromMe) MessagePreviewAvatar(myAvatar)
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

@Composable
private fun MessagePreviewAvatar(model: Any?) {
    AsyncImage(
        model = model,
        contentDescription = null,
        error = androidx.compose.ui.res.painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

private fun Modifier.drawMessageBubbleArrow(
    isFromMe: Boolean,
    color: Color,
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
        }
    }
}
