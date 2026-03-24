package top.chengdongqing.wechat.core.network.messaging

import top.chengdongqing.wechat.core.data.model.ChatProtocol

/**
 * 信令消息分发接口
 */
interface SignalingDispatcher {
    suspend fun onSignalingReceived(protocol: ChatProtocol.Signaling)
}
