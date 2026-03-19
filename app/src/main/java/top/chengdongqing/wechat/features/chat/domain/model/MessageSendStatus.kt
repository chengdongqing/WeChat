package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.data.model.SendError

/**
 * 消息发送状态
 */
sealed class MessageSendStatus {

    /**
     * 发送中
     */
    data class Sending(val progress: Float = 0f) : MessageSendStatus()

    /**
     * 接收中
     */
    data class Receiving(val progress: Float = 0f) : MessageSendStatus()

    /**
     * 已暂停
     */
    data class Paused(val progress: Float = 0f) : MessageSendStatus()

    /**
     * 已发出
     */
    data object Sent : MessageSendStatus()

    /**
     * 已送达
     */
    data object Success : MessageSendStatus()

    /**
     * 发送失败
     */
    data class Failed(val error: SendError) : MessageSendStatus()
}