package top.chengdongqing.wechat.core.model

/**
 * 消息发送状态
 */
enum class SendStatus {
    Sending,        // 发送中
    Receiving,      // 接收中
    Paused,         // 传输暂停
    Sent,           // 已发送
    Delivered,      // 已送达
    Read,           // 已读
    Failed;         // 发送失败

    val isProgressing: Boolean
        get() = this in setOf(Sending, Receiving, Paused)
}

/**
 * 消息发送失败原因
 */
enum class SendError(val canRetry: Boolean) {
    ConnectionFailed(true),
    Cancelled(true),
    NotFriend(false),
    Blocked(false),
    MessageTooLarge(false),
    Unknown(true)
}