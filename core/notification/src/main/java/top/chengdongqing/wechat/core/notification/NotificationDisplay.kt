package top.chengdongqing.wechat.core.notification

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R as DesignR

enum class NotificationDisplay(
    @get:StringRes val descriptionRes: Int
) {
    HiddenAll(DesignR.string.notification_display_hidden_all),
    SenderOnly(DesignR.string.notification_display_sender_only),
    SenderAndContent(DesignR.string.notification_display_sender_and_content);
}
