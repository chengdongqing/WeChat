package top.chengdongqing.wechat.features.call.model

/**
 * 通话状态（当前的）
 */
enum class CallState {
    Idle, Outgoing, Incoming, Connecting, Connected, Ended;

    /**
     * 是否可以通话
     */
    val isTerminal: Boolean
        get() = this == Idle || this == Ended
}