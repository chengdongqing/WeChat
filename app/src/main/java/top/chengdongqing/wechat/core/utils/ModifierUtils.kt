package top.chengdongqing.wechat.core.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import kotlin.math.roundToInt

/**
 * 自定义点击，不带水波纹
 */
fun Modifier.weClickable(
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * 自定义点击，带自定义背景色
 */
fun Modifier.weClickableWithBg(
    showBackground: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedColor = WeChatTheme.colorScheme.divider

    this
        .background(if (isPressed && showBackground) pressedColor else Color.Transparent)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())