package top.chengdongqing.wechat.features.call.domain.model

import top.chengdongqing.wechat.core.util.format
import kotlin.time.Duration.Companion.seconds

/**
 * 通话状态（结果）
 */
enum class CallStatus(val description: String, val descriptionForMe: String) {
    Cancelled("对方已取消", "已取消"),
    Declined("已拒绝", "对方已拒绝"),
    Connected("已接通", "已接通"),
    Missed("未应答", "对方无应答"),
    Failed("", "连接失败");

    companion object {
        fun describeDuration(duration: Long): String {
            return "通话时长 ${duration.seconds.format()}"
        }
    }
}