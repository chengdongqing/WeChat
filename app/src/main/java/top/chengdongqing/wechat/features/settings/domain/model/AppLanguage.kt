package top.chengdongqing.wechat.features.settings.domain.model

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
}