package top.chengdongqing.wechat.data.call.model

sealed class CallState {
    data object Idle : CallState()
    data object Ringing : CallState()    // 响铃（被叫）
    data object Connecting : CallState() // 呼叫中/连接中（主叫）

    // 建议：Active 内部只放接通后的信息，如接通时间点
    data class Active(val startTime: Long) : CallState()

    data object Ended : CallState()
    data class Error(val message: String) : CallState()
}