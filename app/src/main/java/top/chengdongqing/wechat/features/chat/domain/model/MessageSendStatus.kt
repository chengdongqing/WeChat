package top.chengdongqing.wechat.features.chat.domain.model

import top.chengdongqing.wechat.data.model.SendError

/**
 * 消息发送状态的完整生命周期。
 *
 * 正常流转：[Sending] → [Sent] → [Success]
 * 异常流转：[Sending] → [Failed]
 * 暂停续传：[Sending] → [Paused] → [Sending]（断点续传场景）
 *
 * 注意：[Sent] 与 [Success] 是两个不同阶段——
 * [Sent] 表示消息已发送，但尚未确认对方设备收到；
 * [Success] 表示收到了送达回执，确认消息已成功投递。
 */
sealed class MessageSendStatus {
    /** 发送中，[progress] 表示当前上传进度（0.0 ~ 1.0），纯文本消息通常直接跳过此状态。 */
    data class Sending(val progress: Float = 0f) : MessageSendStatus()

    /** 已暂停（断点续传），[progress] 保存暂停时的进度，恢复时从此处继续。 */
    data class Paused(val progress: Float) : MessageSendStatus()

    /** 已发出，等待送达回执。超过 15 秒未收到回执时 UI 层给出提示，见 [ChatMessage.isSent]。 */
    data object Sent : MessageSendStatus()

    /** 已送达，收到回执确认对方设备已接收。 */
    data object Success : MessageSendStatus()

    /** 发送失败，[error] 携带失败原因，UI 层据此决定提示文案和重试策略。 */
    data class Failed(val error: SendError) : MessageSendStatus()
}