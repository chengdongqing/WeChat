package top.chengdongqing.wechat.core.model

enum class HangupReason {
    Normal, Declined, Cancelled, Timeout, Busy, Offline, Error
}

data class HangupResult(
    val reason: HangupReason,
    val isFromMe: Boolean
)
