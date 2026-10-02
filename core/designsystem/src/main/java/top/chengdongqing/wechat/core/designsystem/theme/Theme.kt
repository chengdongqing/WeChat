package top.chengdongqing.wechat.core.designsystem.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Density
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.WeActionSheetHost
import top.chengdongqing.wechat.core.designsystem.components.dialog.WeDialogHost
import top.chengdongqing.wechat.core.designsystem.components.toast.WeToastHost
import top.chengdongqing.wechat.core.designsystem.window.StatusBarAppearance
import top.chengdongqing.wechat.core.model.AppLanguage
import top.chengdongqing.wechat.core.model.AppTheme
import top.chengdongqing.wechat.core.model.DisplaySettings
import java.util.Locale

@Immutable
data class AppearanceSetting(
    val fontScale: Float = 1f,
    val isDarkTheme: Boolean = false,
    val colorScheme: WeColorScheme = LightColorScheme,
    val appLanguage: AppLanguage = AppLanguage.FollowSystem
)

val LocalAppearanceSetting = staticCompositionLocalOf { AppearanceSetting() }

@Composable
fun WeTheme(
    settings: DisplaySettings = DisplaySettings(),
    isDark: Boolean? = null,
    designWidth: Float = 375f,
    content: @Composable () -> Unit
) {
    // 是否启用深色主题
    val isDarkTheme = isDark ?: when (settings.theme) {
        AppTheme.FollowSystem -> isSystemInDarkTheme()
        AppTheme.Dark -> true
        AppTheme.Light -> false
    }
    // 颜色方案
    val colorScheme = when {
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val rippleColor = when {
        isDarkTheme -> Color.White
        else -> Color.Unspecified
    }
    // 语言配置
    val appLanguage = remember(settings.language) {
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

    // 统一在不同屏幕上的显示大小
    val displayWidth = LocalResources.current.displayMetrics.widthPixels
    val density = Density(
        density = displayWidth / designWidth,
        fontScale = LocalDensity.current.fontScale * settings.fontScale.value
    )

    // 设置状态栏文字颜色
    StatusBarAppearance(!isDarkTheme)

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalIndication provides ripple(color = rippleColor),
        LocalAppearanceSetting provides AppearanceSetting(
            isDarkTheme = isDarkTheme,
            colorScheme = colorScheme,
            appLanguage = appLanguage
        )
    ) {
        content()

        FeedbackHosts()
    }
}

@Composable
private fun FeedbackHosts() {
    WeToastHost()
    WeDialogHost()
    WeActionSheetHost()
}

object WeTheme {
    val colorScheme: WeColorScheme
        @Composable
        get() = LocalAppearanceSetting.current.colorScheme
    val typography: Typography
        @Composable
        get() = LocalTypography.current
}