package top.chengdongqing.wechat.data.network.model

/**
 * 通知ID
 */
enum class NotificationId(val id: Int) {
    P2P(1001),
    FriendRequest(2001),
    Call(2002)
}