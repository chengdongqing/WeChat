package top.chengdongqing.wechat.data.network.protocol

import kotlinx.serialization.Serializable

/**
 * P2P消息定义
 */
@Serializable
sealed class P2PMessage {

    /**
     * 好友申请
     */
    @Serializable
    data class FriendRequest(
        val requestId: String,
        val fromUserId: String,
        val fromNickname: String,
        val fromAvatarPath: String?,
        val toUserId: String,
        val greetingMessage: String,
        val remark: String?,
        val tags: List<String>?,
        val note: String?,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 好友申请响应
     */
    @Serializable
    data class FriendRequestResponse(
        val requestId: String,
        val action: RequestAction,
        val remark: String?,
        val tags: List<String>?,
        val note: String?,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 心跳消息
     */
    data class Ping(val timestamp: Long) : P2PMessage()

    /**
     * 心跳响应
     */
    data class Pong(val timestamp: Long) : P2PMessage()
}

@Serializable
enum class RequestAction {
    ACCEPT,
    REJECT
}