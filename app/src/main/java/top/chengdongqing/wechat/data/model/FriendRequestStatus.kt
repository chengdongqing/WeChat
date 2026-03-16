package top.chengdongqing.wechat.data.model

/**
 * 好友请求状态
 */
enum class FriendRequestStatus {
    Pending,    // 待处理
    Accepted,   // 已接受
    Rejected,   // 已拒绝
    Expired     // 已过期
}