package top.chengdongqing.wechat.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 全局字体缩放比例的 CompositionLocal
 */
val LocalFontScale = compositionLocalOf { 1.0f }

/**
 * 扩展函数：自动应用缩放比例
 */
val TextUnit.scaled: TextUnit
    @Composable
    get() = (this.value * LocalFontScale.current).sp