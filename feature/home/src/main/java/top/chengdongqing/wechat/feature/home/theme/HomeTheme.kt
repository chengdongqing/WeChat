package top.chengdongqing.wechat.feature.home.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.designsystem.theme.Dark_TabBar
import top.chengdongqing.wechat.core.designsystem.theme.Grey_F7
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.TabBarIconInactiveDark
import top.chengdongqing.wechat.core.designsystem.theme.White

@Immutable
data class HomeColorScheme(
    val tabBarBackground: Color,
    val tabBarIconInactive: Color,
    val quickActionBackground: Color = Color(0xFF4C4C4C),
    val quickActionText: Color = White
)

private val HomeLightColorScheme = HomeColorScheme(
    tabBarBackground = Grey_F7,
    tabBarIconInactive = Black,
)

private val HomeDarkColorScheme = HomeColorScheme(
    tabBarBackground = Dark_TabBar,
    tabBarIconInactive = TabBarIconInactiveDark,
)

val LocalHomeColorScheme = staticCompositionLocalOf { HomeLightColorScheme }

@Composable
fun HomeTheme(
    isDark: Boolean = LocalAppearanceSetting.current.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) HomeDarkColorScheme else HomeLightColorScheme
    CompositionLocalProvider(LocalHomeColorScheme provides colors) {
        content()
    }
}

object HomeTheme {
    val colorScheme: HomeColorScheme
        @Composable get() = LocalHomeColorScheme.current
}