package top.chengdongqing.wechat.ui.call.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 圆形控制按钮
 *
 * 通用的圆形按钮组件，支持激活状态切换和自定义样式
 *
 * @param iconResId 图标资源ID
 * @param text 按钮文本
 * @param backgroundColor 背景颜色
 * @param isActive 是否激活（影响颜色反转）
 * @param onClick 点击事件
 * @param buttonSize 按钮大小
 * @param iconSize 图标大小
 */
@Composable
fun CircularControlButton(
    @DrawableRes iconResId: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    isActive: Boolean = false,
    buttonSize: Dp = 64.dp,
    iconSize: Dp = 36.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        // 按钮圆圈（带动画过渡）
        Crossfade(
            targetState = isActive,
            label = "button_state_animation"
        ) { active ->
            ButtonCircle(
                iconResId = iconResId,
                contentDescription = text,
                backgroundColor = if (active) Color.White else backgroundColor,
                iconTint = if (active) Color.Black else Color.White,
                size = buttonSize,
                iconSize = iconSize,
                onClick = onClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮文本
        ButtonLabel(text = text)
    }
}

/**
 * 按钮圆圈
 */
@Composable
private fun ButtonCircle(
    @DrawableRes iconResId: Int,
    contentDescription: String,
    backgroundColor: Color,
    iconTint: Color,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * 按钮标签
 */
@Composable
private fun ButtonLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
