package top.chengdongqing.wechat.core.designsystem.ui

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.SendError

@get:StringRes
val SendError.messageRes: Int
    get() = when (this) {
        SendError.ConnectionFailed -> R.string.send_error_connection_failed
        SendError.Cancelled -> R.string.send_error_cancelled
        SendError.NotFriend -> R.string.send_error_not_friend
        SendError.Blocked -> R.string.send_error_blocked
        SendError.MessageTooLarge -> R.string.send_error_too_large
        SendError.Unknown -> R.string.send_error_unknown
    }
