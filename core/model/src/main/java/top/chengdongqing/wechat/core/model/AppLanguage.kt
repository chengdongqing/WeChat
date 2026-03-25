package top.chengdongqing.wechat.core.model

/**
 * 语言
 */
enum class AppLanguage(val locale: String?) {
    FollowSystem(null),
    Chinese("zh"),
    English("en");

    companion object {
        fun fromName(name: String?): AppLanguage =
            entries.find { it.name == name } ?: FollowSystem
    }
}
