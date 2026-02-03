package top.chengdongqing.wechat.core.designsystem.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    val window = (view.context as Activity).window
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
fun ImmersiveModeEffect(enabled: Boolean = true) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)

    DisposableEffect(enabled) {
        if (enabled) {
            // 让状态栏区域可用于布局
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 隐藏系统状态栏、导航栏等
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            // 当手动将状态栏或导航栏滑出后只短暂显示一会儿就收起
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
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