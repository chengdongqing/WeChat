package top.chengdongqing.wechat.data.network.connection

/**
 * 连接事件
 */
sealed class ConnectionEvent {
    /**
     * TCP 连接已建立并完成握手
     */
    data class Connected(val userId: String, val conn: PeerConnection) : ConnectionEvent()

    /**
     * 连接已断开
     *
     * @param reason 说明原因（主动断开 / Pong 超时 / 接收异常等）
     */
    data class Disconnected(val userId: String, val reason: String?) : ConnectionEvent()
}