package top.chengdongqing.wechat.features.call.domain.model

import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 通话类型
 */
enum class CallType(@get:StringRes val labelRes: Int) {
    Voice(R.string.call_type_voice),
    Video(R.string.call_type_video);

    val isVideoCall: Boolean get() = this == Video
}