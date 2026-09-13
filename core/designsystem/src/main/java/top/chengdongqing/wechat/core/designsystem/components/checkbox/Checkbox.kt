package top.chengdongqing.wechat.core.designsystem.components.checkbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun WeCheckBox(
    checked: Boolean,
    enabled: Boolean = true
) {
    val bgColor = getBackgroundColor(checked, enabled)

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(
                width = if (checked) Dp.Unspecified else 1.dp,
                color = WeTheme.colorScheme.divider,
                shape = CircleShape
            )
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = if (checked) Color.White else Color.Transparent
        )
    }
}

@Composable
private fun getBackgroundColor(
    checked: Boolean,
    enabled: Boolean
): Color {
    val isDarkTheme = LocalAppearanceSetting.current.isDarkTheme

    return when (enabled) {
        true -> if (checked) {
            WeTheme.colorScheme.primary
        } else {
            Color.Transparent
        }

        else ->
            if (isDarkTheme) {
                Color(0xFFBBBBBB).copy(alpha = 0.4f)
            } else {
                Color(0xFFBBBBBB)
            }
    }
}
