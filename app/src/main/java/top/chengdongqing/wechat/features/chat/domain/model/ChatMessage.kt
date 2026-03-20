package top.chengdongqing.wechat.features.chat.domain.model

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.data.model.SendError

/**
 * 聊天消息的完整描述，包含消息元数据和当前发送状态。
 */
@Immutable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val content: MessageContent,
    val isRecalled: Boolean = false,
    val isFromMe: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val sendStatus: MessageSendStatus = MessageSendStatus.Success
) {
    val isProgressing: Boolean
        get() = sendStatus is MessageSendStatus.Sending || sendStatus is MessageSendStatus.Receiving || sendStatus is MessageSendStatus.Paused

    /**
     * 消息已发出但尚未收到送达回执。
     */
    val isSent: Boolean
        get() = sendStatus is MessageSendStatus.Sent && !timestamp.isWithinSeconds(15)

    val isFailed: Boolean
        get() = sendStatus is MessageSendStatus.Failed

    /**
     * 当前上传/发送进度，范围 0.0 ~ 1.0。
     */
    val sendProgress: Float
        get() = when (sendStatus) {
            is MessageSendStatus.Sending -> sendStatus.progress
            is MessageSendStatus.Receiving -> sendStatus.progress
            is MessageSendStatus.Paused -> sendStatus.progress
            else -> 0f
        }

    /**
     * 发送失败的具体原因，仅 [MessageSendStatus.Failed] 状态下非空。
     * UI 层可用于展示错误提示文案或决定是否允许重试。
     */
    val error: SendError?
        get() = (sendStatus as? MessageSendStatus.Failed)?.error
}