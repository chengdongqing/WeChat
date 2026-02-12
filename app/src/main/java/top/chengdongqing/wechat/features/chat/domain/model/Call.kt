package top.chengdongqing.wechat.features.chat.domain.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.util.format
import kotlin.time.Duration.Companion.milliseconds

/**
 * 通话记录状态
 */
enum class CallStatus(val description: String, val descriptionForMe: String) {
    Cancelled("对方已取消", "已取消"),
    Rejected("对方已拒绝", "已拒绝"),
    Connected("已接通", "已接通"),
    Missed("未应答", "对方无应答");

    companion object {
        fun describeDuration(duration: Long): String {
            return "通话时长 ${duration.milliseconds.format()}"
        }
    }
}

/**
 * 通话类型
 */
@Serializable
enum class CallType(val label: String) {
    Voice("语音通话"),
    Video("视频通话");

    val isVideoCall: Boolean get() = this == Video
}