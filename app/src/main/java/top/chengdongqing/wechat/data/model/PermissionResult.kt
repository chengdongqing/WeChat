package top.chengdongqing.wechat.data.model

enum class PermissionResult {
    Allowed,

    /**
     * 被拉黑
     */
    Blocked,

    /**
     * 非好友
     */
    NotFriend,

    /**
     * 数据包签名验证失败
     */
    InvalidSignature
}