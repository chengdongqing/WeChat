package top.chengdongqing.wechat.features.settings.domain.model

import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题
 */
enum class AppTheme(
    val label: String,
    val mode: Int
) {
    FollowSystem("跟随系统", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    Light("普通模式", AppCompatDelegate.MODE_NIGHT_NO),
    Dark("深色模式", AppCompatDelegate.MODE_NIGHT_YES);

    val isFollowSystem: Boolean
        get() = this == FollowSystem

    companion object {
        fun fromName(name: String?): AppTheme {
            return entries.find { it.name == name } ?: FollowSystem
        }
    }
}