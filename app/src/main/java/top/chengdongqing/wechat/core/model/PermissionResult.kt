package top.chengdongqing.wechat.core.model

/**
 * 权限校验结果
 */
enum class PermissionResult {
    /**
     * 检查通过
     */
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