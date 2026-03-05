package top.chengdongqing.wechat.features.call.model

/**
 * 通话状态（当前的）
 */
enum class CallState {
    Idle, Outgoing, Incoming, Connecting, Connected, Ended
}