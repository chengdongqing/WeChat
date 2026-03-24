package top.chengdongqing.wechat.core.model

sealed class MessageSendStatus {
    data class Sending(val progress: Float = 0f) : MessageSendStatus()
    data class Receiving(val progress: Float = 0f) : MessageSendStatus()
    data class Paused(val progress: Float = 0f) : MessageSendStatus()
    data object Sent : MessageSendStatus()
    data object Delivered : MessageSendStatus()
    data class Failed(val error: SendError) : MessageSendStatus()
}
