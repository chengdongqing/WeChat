package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReceiptType {
    Delivered,
    Read,
    Recalled,
    Blocked,
    NotFriend,
    InvalidSignature
}
