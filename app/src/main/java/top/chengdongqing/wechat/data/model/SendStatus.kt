package top.chengdongqing.wechat.data.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

enum class SendStatus {
    Sending,        // 发送中
    Sent,           // 已发送
    Delivered,      // 已送达
    Read,           // 已读
    Failed,         // 发送失败
    Receiving,      // 接收中
    Paused          // 传输暂停
}

enum class SendError(
    @get:StringRes val messageRes: Int,
    val canRetry: Boolean
) {
    ConnectionFailed(R.string.send_error_connection_failed, true),
    Cancelled(R.string.send_error_cancelled, true),
    NotFriend(R.string.send_error_not_friend, false),
    Blocked(R.string.send_error_blocked, false),
    MessageTooLarge(R.string.send_error_too_large, false),
    Unknown(R.string.send_error_unknown, true)
}