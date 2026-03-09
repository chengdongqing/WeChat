package top.chengdongqing.wechat.features.settings.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 通知提示音
 */
enum class NotificationSound(
    @get:StringRes val labelRes: Int,
    val soundRes: Int
) {
    FollowSystem(R.string.notification_sound_follow_system, R.raw.sent_message),
    Blocks(R.string.notification_sound_blocks, R.raw.sent_message),
    Cute(R.string.notification_sound_cute, R.raw.sent_message),
    Ethereal(R.string.notification_sound_ethereal, R.raw.sent_message),
    Playful(R.string.notification_sound_playful, R.raw.sent_message),
    Crisp(R.string.notification_sound_crisp, R.raw.sent_message),
    Nimble(R.string.notification_sound_nimble, R.raw.sent_message),
    Elegant(R.string.notification_sound_elegant, R.raw.sent_message);
}