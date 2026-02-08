package top.chengdongqing.wechat.data.network.protocol

import java.io.Serializable

/**
 * P2P消息定义
 */
sealed class P2PMessage : Serializable {

    /**
     * 获取用户资料请求
     */
    data class GetProfileRequest(
        val userId: String,
        val requesterId: String,
        val includeAvatar: Boolean = true
    ) : P2PMessage()

    /**
     * 获取用户资料响应
     */
    data class GetProfileResponse(
        val userId: String,
        val nickname: String,
        val gender: Int?,
        val signature: String?,
        val avatarData: ByteArray?  // 头像二进制数据
    ) : P2PMessage()

    /**
     * 好友申请
     */
    data class FriendRequest(
        val requestId: String,
        val fromUserId: String,
        val fromNickname: String,
        val toUserId: String,
        val verificationMessage: String,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 好友申请响应
     */
    data class FriendRequestResponse(
        val requestId: String,
        val userId: String,
        val action: RequestAction,
        val myProfile: GetProfileResponse? = null  // 接受时返回自己的资料
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

enum class RequestAction {
    ACCEPT,
    REJECT
}