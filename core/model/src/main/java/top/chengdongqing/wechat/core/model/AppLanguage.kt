package top.chengdongqing.wechat.core.model

import androidx.annotation.StringRes

/**
 * 语言
 */
enum class AppLanguage(
    @get:StringRes val labelRes: Int,
    val locale: String?
) {
    FollowSystem(R.string.settings_follow_system, null),
    Chinese(R.string.display_language_chinese, "zh"),
    English(R.string.display_language_english, "en");

    companion object {
        fun fromName(name: String?): AppLanguage =
            entries.find { it.name == name } ?: FollowSystem
    }
}
