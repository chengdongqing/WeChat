package top.chengdongqing.wechat.core.data.model

import top.chengdongqing.wechat.core.common.util.isWithinSeconds
import top.chengdongqing.wechat.core.model.MessageSendStatus
import top.chengdongqing.wechat.core.model.SendError

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val content: MessageContent,
    val isRecalled: Boolean = false,
    val isFromMe: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val sendStatus: MessageSendStatus
) {
    val isProgressing: Boolean
        get() = sendStatus is MessageSendStatus.Sending
                || sendStatus is MessageSendStatus.Receiving
                || sendStatus is MessageSendStatus.Paused

    val isSent: Boolean
        get() = sendStatus is MessageSendStatus.Sent && !timestamp.isWithinSeconds(15)

    val isFailed: Boolean
        get() = sendStatus is MessageSendStatus.Failed

    val sendProgress: Float
        get() = when (sendStatus) {
            is MessageSendStatus.Sending -> sendStatus.progress
            is MessageSendStatus.Receiving -> sendStatus.progress
            is MessageSendStatus.Paused -> sendStatus.progress
            else -> 0f
        }

    val error: SendError?
        get() = (sendStatus as? MessageSendStatus.Failed)?.error
}
