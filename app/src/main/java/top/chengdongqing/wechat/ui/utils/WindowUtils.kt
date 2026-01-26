package top.chengdongqing.wechat.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
fun rememberWindowFractionWidth(fraction: Float = 0.5f): Dp {
    val windowInfo = LocalWindowInfo.current

    return remember(windowInfo, fraction) {
        windowInfo.containerDpSize.width * fraction
    }
}