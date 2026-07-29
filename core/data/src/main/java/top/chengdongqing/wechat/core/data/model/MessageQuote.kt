package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.model.MessageType

/**
 * An immutable snapshot of the quoted message. Keeping a snapshot means a quote remains useful
 * even when the original message is later recalled, deleted, or is not loaded on this device.
 */
@Serializable
data class MessageQuote(
    val messageId: String,
    val senderId: String,
    val messageType: MessageType,
    val preview: String
)
