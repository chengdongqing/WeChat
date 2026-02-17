package top.chengdongqing.wechat.features.call.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.util.weClickable

/**
 * 统一的通话控制按钮
 *
 * @param isActive 为 null 时表示普通动作按钮（如挂断），为 true/false 时表示状态开关（如麦克风）
 */
@Composable
fun ControlToggle(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit,
    isActive: Boolean? = null,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    activeColor: Color = Color.White,
    iconTint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    when (isActive) {
                        true -> activeColor
                        else -> backgroundColor
                    }
                )
                .weClickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                // 如果是激活状态，图标颜色通常反转为黑色
                tint = if (isActive == true) Color.Black else iconTint
            )
        }

        label?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.White, fontSize = 12.sp)
        }
    }
}