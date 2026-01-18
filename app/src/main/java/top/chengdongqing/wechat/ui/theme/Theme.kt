package top.chengdongqing.wechat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class WeChatColorScheme(
    val primary: Color,
    val primaryPressed: Color,
    val divider: Color,
    val textPrimary: Color,
    val backgroundDefault: Color,
    val tabBarBackground: Color,
    val tabBarIconInactive: Color,
    val redDot: Color = Color.Red
)

// 浅色主题配置
private val LightColorScheme = WeChatColorScheme(
    primary = GreenPrimary,
    primaryPressed = GreenPressed,
    divider = DividerLight,
    textPrimary = TextPrimaryLight,
    backgroundDefault = WeChatBgLight,
    tabBarBackground = TabBarBgLight,
    tabBarIconInactive = Black
)

// 深色主题配置
private val DarkColorScheme = WeChatColorScheme(
    primary = GreenPrimary,
    primaryPressed = GreenPressed,
    divider = DividerDark,
    textPrimary = TextPrimaryDark,
    backgroundDefault = WeChatBgDark,
    tabBarBackground = TabBarBgDark,
    tabBarIconInactive = IconInactiveDark
)

val LocalWeChatColorScheme = staticCompositionLocalOf { LightColorScheme }

@Composable
fun WeChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalWeChatColorScheme provides colorScheme) {
        MaterialTheme(
            typography = Typography,
            content = content
        )
    }
}

object WeChatTheme {
    val colorScheme: WeChatColorScheme
        @Composable
        get() = LocalWeChatColorScheme.current
}