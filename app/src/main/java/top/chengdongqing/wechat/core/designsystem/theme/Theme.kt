package top.chengdongqing.wechat.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme
import top.chengdongqing.wechat.features.settings.ui.display.DisplaySettingsViewModel
import java.util.Locale

@Immutable
data class WeColorScheme(
    val primary: Color = GreenPrimary,
    val danger: Color = Danger,
    val link: Color = LinkColor,
    // 背景层级（从低到高）
    val background: Color,       // 页面底色
    val surface: Color,          // 卡片/列表容器
    val surfaceVariant: Color,   // 输入框/次级容器
    val elevated: Color,         // 浮层/弹窗
    // 文本层级
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,     // 时间戳、占位符等
    // 其他
    val divider: Color,          // 分隔线
)

val LightColorScheme = WeColorScheme(
    background = Grey_ED,
    surface = White,
    surfaceVariant = Grey_F7,
    elevated = White,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    divider = DividerLight,
)

val DarkColorScheme = WeColorScheme(
    background = Dark_BG,
    surface = Dark_Surface,
    surfaceVariant = Dark_Surface2,
    elevated = Dark_Elevated,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    divider = DividerDark,
)

val LocalWeColorScheme = staticCompositionLocalOf { LightColorScheme }
val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FollowSystem }

@Composable
fun WeTheme(
    viewModel: DisplaySettingsViewModel = hiltViewModel(),
    isDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    // 读取显示配置
    val settings by viewModel.settings.collectAsState()

    // 是否启用深色主题
    val isDarkTheme = isDark ?: when (settings.theme) {
        AppTheme.FollowSystem -> isSystemInDarkTheme()
        AppTheme.Dark -> true
        AppTheme.Light -> false
    }
    val colorScheme = when {
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 当前的语言
    val currentLanguage = remember(settings.language) {
        when (settings.language) {
            AppLanguage.FollowSystem -> {
                val locale = AppCompatDelegate.getApplicationLocales()[0]
                    ?: Locale.getDefault()
                AppLanguage.entries.find { it.locale == locale.language }
                    ?: AppLanguage.FollowSystem
            }

            else -> settings.language
        }
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
                platformStyle = PlatformTextStyle(false) // 避免文本自带边距
            ),
            LocalFontScale provides settings.fontScale.scale,
            LocalIsDarkTheme provides isDarkTheme,
            LocalWeColorScheme provides colorScheme,
            LocalAppLanguage provides currentLanguage
        ) {
            content()
        }
    }
}

object WeTheme {
    val colorScheme: WeColorScheme
        @Composable
        get() = LocalWeColorScheme.current
}