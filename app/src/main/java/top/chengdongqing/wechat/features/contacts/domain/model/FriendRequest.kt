package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.model.FriendRequestStatus
import top.chengdongqing.wechat.features.me.domain.model.Gender

data class FriendRequest(
    val id: String,
    val peerUserId: String,
    val peerNickname: String,
    val peerAvatarPath: String?,
    val greetingMessage: String,
    val remark: String?,
    val status: FriendRequestStatus,
    val direction: RequestDirection,
    val timestamp: Long
)

data class FriendProfileResponse(
    val userId: String,
    val nickname: String,
    val signature: String?,
    val gender: Gender?,
    val avatarData: ByteArray?,
    val publicKey: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FriendProfileResponse

        if (userId != other.userId) return false
        if (nickname != other.nickname) return false
        if (signature != other.signature) return false
        if (gender != other.gender) return false
        if (!avatarData.contentEquals(other.avatarData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + nickname.hashCode()
        result = 31 * result + (signature?.hashCode() ?: 0)
        result = 31 * result + (gender?.hashCode() ?: 0)
        result = 31 * result + (avatarData?.contentHashCode() ?: 0)
        return result
    }
}

data class FriendRequestResponse(
    val requestId: String,
    val accepted: Boolean
)

data class IncomingFriendRequest(
    val requestId: String,
    val peerUserId: String,
    val peerNickname: String,
    val peerPublicKey: String,
    val greetingMessage: String,
    val avatarData: ByteArray?,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncomingFriendRequest

        if (timestamp != other.timestamp) return false
        if (requestId != other.requestId) return false
        if (peerUserId != other.peerUserId) return false
        if (peerNickname != other.peerNickname) return false
        if (greetingMessage != other.greetingMessage) return false
        if (!avatarData.contentEquals(other.avatarData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + requestId.hashCode()
        result = 31 * result + peerUserId.hashCode()
        result = 31 * result + peerNickname.hashCode()
        result = 31 * result + greetingMessage.hashCode()
        result = 31 * result + (avatarData?.contentHashCode() ?: 0)
        return result
    }
}