package top.chengdongqing.wechat.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import top.chengdongqing.wechat.ui.theme.WeChatTheme

/**
 * 点击不带水波纹
 */
fun Modifier.weClickable(
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null, // 移除水波纹
        onClick = onClick
    )
}

/**
 * 点击带自定义背景色
 */
fun Modifier.weClickableWithBg(
    showBackground: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按下时的背景色
    val pressedColor = WeChatTheme.colorScheme.divider

    this
        .background(if (isPressed && showBackground) pressedColor else Color.Transparent)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}