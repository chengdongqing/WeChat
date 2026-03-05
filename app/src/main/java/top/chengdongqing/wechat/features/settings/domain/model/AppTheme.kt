package top.chengdongqing.wechat.features.settings.domain.model

/**
 * 主题
 */
enum class AppTheme(
    val label: String
) {
    FollowSystem("跟随系统"),
    Light("普通模式"),
    Dark("深色模式");

    val isFollowSystem: Boolean
        get() = this == FollowSystem
}