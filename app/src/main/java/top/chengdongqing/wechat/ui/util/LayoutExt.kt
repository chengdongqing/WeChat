package top.chengdongqing.wechat.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/**
 * 获取窗口宽度的百分比尺寸
 */
@Composable
fun rememberScreenFractionWidth(fraction: Float = 0.5f): Dp {
    val windowInfo = LocalWindowInfo.current
    return remember(windowInfo, fraction) {
        windowInfo.containerDpSize.width * fraction
    }
}