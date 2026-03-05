package top.chengdongqing.wechat.features.settings.domain.model

import top.chengdongqing.wechat.R

/**
 * 通知提示音
 */
enum class NotificationSound(
    val label: String,
    val soundResId: Int
) {
    FollowSystem("跟随系统", R.raw.sent_message),
    Blocks("积木", R.raw.sent_message),
    Cute("可爱", R.raw.sent_message),
    Ethereal("空灵", R.raw.sent_message),
    Playful("俏皮", R.raw.sent_message),
    Crisp("清脆", R.raw.sent_message),
    Nimble("灵动", R.raw.sent_message),
    Elegant("优雅", R.raw.sent_message);
}