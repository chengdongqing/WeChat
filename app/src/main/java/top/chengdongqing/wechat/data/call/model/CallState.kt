package top.chengdongqing.wechat.data.call.model

/**
 * 通话状态
 */
sealed class CallState {
    /** 空闲状态 */
    object Idle : CallState()

    /** 连接中（呼出时） */
    object Connecting : CallState()

    /** 响铃中（来电时） */
    object Ringing : CallState()

    /** 通话中 */
    data class Active(val startTime: Long) : CallState()

    /** 已结束 */
    object Ended : CallState()

    /** 连接失败 */
    data class Failed(val reason: String) : CallState()
}