package top.chengdongqing.wechat.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

@Immutable
class WeColorScheme(
    val primary: Color = GreenPrimary,
    val primaryPressed: Color = GreenPressed,
    // 基础表面
    val background: Color,       // 页面底色
    val surface: Color,          // 容器/卡片色
    val surfaceVariant: Color,   // 次要容器色
    // 内容色
    val textPrimary: Color,
    val textSecondary: Color,    // 副文本色
    val link: Color = LinkColor,
    val divider: Color,
    // 特定组件色
    val tabBarBackground: Color,
    val tabBarIconInactive: Color,
    val error: Color = Danger
)

private val LightColorScheme = WeColorScheme(
    background = Grey_ED,
    surface = White,
    surfaceVariant = Grey_F7,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    divider = DividerLight,
    tabBarBackground = Grey_F7,
    tabBarIconInactive = Black
)

private val DarkColorScheme = WeColorScheme(
    background = Black,
    surface = Grey_4C,
    surfaceVariant = Grey_2B,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    divider = DividerDark,
    tabBarBackground = Grey_19,
    tabBarIconInactive = TabBarIconInactiveDark
)

val LocalWeColorScheme = staticCompositionLocalOf { LightColorScheme }

@Composable
fun WeTheme(
    darkTheme: Boolean = false, // isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                // 设置状态栏的背景颜色为透明
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                // 设置为 false 表示 App 内容会延伸到状态栏和导航栏的正下方（即沉浸式）
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    MaterialTheme {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                // 避免文本自带边距
                platformStyle = PlatformTextStyle(false)
            )
        ) {
            CompositionLocalProvider(LocalWeColorScheme provides colorScheme) {
                content()
            }
        }
    }
}

object WeTheme {
    val colorScheme: WeColorScheme
        @Composable
        get() = LocalWeColorScheme.current
}