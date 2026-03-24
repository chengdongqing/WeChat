package top.chengdongqing.wechat.core.model

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题
 */
enum class AppTheme(
    @get:StringRes val labelRes: Int,
    val mode: Int
) {
    FollowSystem(R.string.settings_follow_system, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    Light(R.string.display_theme_light, AppCompatDelegate.MODE_NIGHT_NO),
    Dark(R.string.display_theme_dark, AppCompatDelegate.MODE_NIGHT_YES);

    val isFollowSystem get() = this == FollowSystem

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.find { it.name == name } ?: FollowSystem
    }
}
