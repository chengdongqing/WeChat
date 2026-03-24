package top.chengdongqing.wechat.core.common.notification

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.common.R

enum class NotificationDisplay(
    @get:StringRes val descriptionRes: Int
) {
    HiddenAll(R.string.notification_display_hidden_all),
    SenderOnly(R.string.notification_display_sender_only),
    SenderAndContent(R.string.notification_display_sender_and_content);
}
