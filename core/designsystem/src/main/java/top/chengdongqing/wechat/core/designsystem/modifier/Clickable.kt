package top.chengdongqing.wechat.core.designsystem.modifier

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * 没有视觉反馈的点击事件
 */
fun Modifier.onTap(
    enabled: Boolean = true,
    role: Role? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    role = role,
    onClickLabel = onClickLabel,
    onClick = onClick
)
