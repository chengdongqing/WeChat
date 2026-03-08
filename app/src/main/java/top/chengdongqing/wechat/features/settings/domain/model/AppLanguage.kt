package top.chengdongqing.wechat.features.settings.domain.model

import java.util.Locale

/**
 * 语言
 */
enum class AppLanguage(
    val label: String,
    val locale: String?
) {
    FollowSystem("跟随系统", null),
    Chinese("简体中文", "zh"),
    English("English", "en");

    companion object {
        fun fromName(name: String?): AppLanguage {
            return entries.find { it.name == name } ?: FollowSystem
        }

        fun fromSystemLocale(): AppLanguage {
            val lang = Locale.getDefault().language
            return entries.find { it.locale == lang } ?: FollowSystem
        }
    }
}