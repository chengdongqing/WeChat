package top.chengdongqing.wechat.data.network.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.data.model.ContactAddSource

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
        val source: ContactAddSource?,
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
}