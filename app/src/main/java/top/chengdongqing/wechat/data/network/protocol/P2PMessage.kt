package top.chengdongqing.wechat.data.network.protocol

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.features.me.domain.model.Gender

/**
 * P2P消息协议
 */
@Serializable
sealed class P2PMessage {

    /**
     * 好友申请
     */
    @Serializable
    data class FriendRequest(
        val requestId: String,
        val peerUserId: String,
        val peerNickname: String,
        val greetingMessage: String,
        val avatarSize: Int,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 好友申请响应
     */
    @Serializable
    data class FriendRequestResponse(
        val requestId: String,
        val action: RequestAction,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 完整资料响应
     */
    @Serializable
    data class FullProfileResponse(
        val requestId: String,
        val userId: String,
        val nickname: String,
        val signature: String?,
        val gender: Gender?,
        val avatarSize: Int,
        val timestamp: Long
    ) : P2PMessage()

    /**
     * 自动添加响应（我删除了对方，但对方还保留着我，这时我再次申请添加他）
     */
    @Serializable
    data class AutoAddResponse(
        val requestId: String,
        val userId: String,
        val nickname: String,
        val signature: String?,
        val gender: Gender?,
        val avatarSize: Int,
        val timestamp: Long
    ) : P2PMessage()
}

@Serializable
enum class RequestAction {
    ACCEPT,
    REJECT
}