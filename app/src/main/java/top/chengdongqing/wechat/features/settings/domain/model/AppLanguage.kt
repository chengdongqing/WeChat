package top.chengdongqing.wechat.features.settings.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

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