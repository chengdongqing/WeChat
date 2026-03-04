package top.chengdongqing.wechat.data.model

enum class SendStatus {
    Sending,        // 发送中
    Sent,           // 已发送
    Delivered,      // 已送达
    Read,           // 已读
    Failed          // 发送失败
}

enum class SendError(val message: String, val canRetry: Boolean) {
    ConnectionFailed("连接失败。", true),
    Cancelled("已取消发送。", true),
    NotFriend(
        "对方开启了朋友验证，你还不是他（她）朋友。请先发送朋友验证，对方验证通过后，才能聊天。",
        false
    ),
    Blocked("消息已发出，但被对方拒收了。", false),
    MessageTooLarge("消息内容过大。", false),
    Unknown("未知错误。", true)
}