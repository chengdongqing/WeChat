package top.chengdongqing.wechat.ui.components.divider

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun WeDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 0.5.dp,
    color: Color = WeChatTheme.colorScheme.divider,
    direction: DividerDirection = DividerDirection.HORIZONTAL
) {
    if (direction == DividerDirection.HORIZONTAL) {
        HorizontalDivider(
            modifier = modifier,
            thickness = thickness,
            color = color
        )
    } else {
        VerticalDivider(
            modifier = modifier,
            thickness = thickness,
            color = color
        )
    }
}

enum class DividerDirection {
    HORIZONTAL,
    VERTICAL
}