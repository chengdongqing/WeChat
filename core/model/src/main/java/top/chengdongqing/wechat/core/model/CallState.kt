package top.chengdongqing.wechat.core.model

enum class CallState {
    Idle, Outgoing, Incoming, Connecting, Connected, Ended;

    val isTerminal: Boolean
        get() = this in setOf(Idle, Ended)
}
