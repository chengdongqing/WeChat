package top.chengdongqing.wechat.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/**
 * 没有视觉反馈的点击事件
 */
fun Modifier.onTap(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClick = onClick
)
