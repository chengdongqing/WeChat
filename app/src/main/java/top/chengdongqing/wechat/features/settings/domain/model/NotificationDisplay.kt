package top.chengdongqing.wechat.features.settings.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 通知显示方式
 */
enum class NotificationDisplay(
    @get:StringRes val descriptionRes: Int
) {
    HiddenAll(R.string.notification_display_hidden_all),
    SenderOnly(R.string.notification_display_sender_only),
    SenderAndContent(R.string.notification_display_sender_and_content);
}