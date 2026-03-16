package top.chengdongqing.wechat.data.network.model

/**
 * 加好友相关事件
 */
sealed class FriendEvent {

    /**
     * 加好友请求
     */
    data class FriendRequest(
        val nickname: String,
        val message: String
    ) : FriendEvent()

    /**
     * 加好友结果
     */
    data class FriendResponse(
        val result: FriendRequestResult
    ) : FriendEvent()

    /**
     * 添加成功
     */
    data class Added(
        val nickname: String
    ) : FriendEvent()
}