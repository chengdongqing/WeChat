package top.chengdongqing.wechat.core.designsystem.components.divider

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun WeDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 0.5.dp,
    color: Color = WeTheme.colorScheme.divider,
    orientation: Orientation = Orientation.Horizontal
) {
    if (orientation == Orientation.Horizontal) {
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