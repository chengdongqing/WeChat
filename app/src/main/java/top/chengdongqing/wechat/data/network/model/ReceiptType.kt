package top.chengdongqing.wechat.data.network.model

/**
 * 回执类型
 */
enum class ReceiptType {
    Delivered, // 送达
    Read, // 已读
    Recalled, // 已撤回
    Blocked, // 拉黑拒收
    NotFriend, // 非好友拒收
    InvalidSignature // 验签失败
}