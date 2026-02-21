package top.chengdongqing.wechat.core.designsystem.util

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 设置状态栏文字颜色
 */
@Composable
fun StatusBarAppearanceEffect(isDark: Boolean = true) {
    val view = LocalView.current
    val window = LocalActivity.current?.window ?: return
    val insetsController = remember { WindowCompat.getInsetsController(window, view) }
    val initialStyle = remember { insetsController.isAppearanceLightStatusBars }

    DisposableEffect(isDark) {
        insetsController.isAppearanceLightStatusBars = isDark
        onDispose {
            insetsController.isAppearanceLightStatusBars = initialStyle
        }
    }
}

/**
 * 设置全屏模式
 */
@Composable
fun ImmersiveSystemBars(enabled: Boolean = true) {
    val window = LocalActivity.current?.window ?: return

    LaunchedEffect(enabled) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (!enabled) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

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