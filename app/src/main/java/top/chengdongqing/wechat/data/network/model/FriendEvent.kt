package top.chengdongqing.wechat.data.network.model

/**
 * 加好友相关事件
 */
sealed class FriendEvent {

    /**
     * 加好友请求
     */
    data class FriendRequest(
        val nickname: String
    ) : FriendEvent()

    /**
     * 添加成功
     */
    data class Added(
        val nickname: String,
        val contactId: String
    ) : FriendEvent()
}