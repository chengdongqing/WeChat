package top.chengdongqing.wechat.data.network.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.features.me.domain.model.Gender

/**
 * 加好友消息协议
 */
@Serializable
sealed class FriendProtocol {

    /**
     * 好友申请
     */
    @Serializable
    data class FriendRequest(
        val requestId: String,
        val userId: String,
        val nickname: String,
        val publicKey: String,
        val greeting: String,
        val avatarSize: Int,
        val timestamp: Long
    ) : FriendProtocol()

    /**
     * 好友申请响应
     */
    @Serializable
    data class FriendResponse(
        val requestId: String,
        val result: FriendRequestResult,
        val timestamp: Long
    ) : FriendProtocol()

    /**
     * 完整资料请求
     */
    @Serializable
    data class ProfileRequest(
        val timestamp: Long = System.currentTimeMillis()
    ) : FriendProtocol()

    /**
     * 完整资料响应
     */
    @Serializable
    data class ProfileResponse(
        val requestId: String,
        val userId: String,
        val nickname: String,
        val signature: String?,
        val gender: Gender?,
        val avatarSize: Int,
        val publicKey: String,
        val timestamp: Long
    ) : FriendProtocol()
}