package top.chengdongqing.wechat.features.call.domain.model

/**
 * 挂断原因
 */
enum class HangupReason {
    Normal, Declined, Cancelled, Timeout, Busy, Offline, Error
}

data class HangupResult(
    val reason: HangupReason,
    val isFromMe: Boolean
)