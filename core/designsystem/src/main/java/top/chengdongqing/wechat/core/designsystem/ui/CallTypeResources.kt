package top.chengdongqing.wechat.core.designsystem.ui

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.CallType

@get:StringRes
val CallType.labelRes: Int
    get() = when (this) {
        CallType.Voice -> R.string.call_type_voice
        CallType.Video -> R.string.call_type_video
    }
