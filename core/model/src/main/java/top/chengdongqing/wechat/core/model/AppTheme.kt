package top.chengdongqing.wechat.core.model

import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题
 */
enum class AppTheme(val mode: Int) {
    FollowSystem(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    Light(AppCompatDelegate.MODE_NIGHT_NO),
    Dark(AppCompatDelegate.MODE_NIGHT_YES);

    val isFollowSystem get() = this == FollowSystem

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.find { it.name == name } ?: FollowSystem
    }
}
