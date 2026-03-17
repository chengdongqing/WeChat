package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.model.FriendRequestStatus
import top.chengdongqing.wechat.features.me.domain.model.Gender

data class FriendRequest(
    val id: String,
    val userId: String,
    val nickname: String,
    val avatarPath: String?,
    val greeting: String,
    val remark: String?,
    val status: FriendRequestStatus,
    val isFromMe: Boolean,
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
    val userId: String,
    val nickname: String,
    val publicKey: String,
    val greeting: String,
    val avatarData: ByteArray?,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncomingFriendRequest

        if (timestamp != other.timestamp) return false
        if (requestId != other.requestId) return false
        if (userId != other.userId) return false
        if (nickname != other.nickname) return false
        if (greeting != other.greeting) return false
        if (!avatarData.contentEquals(other.avatarData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + requestId.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + nickname.hashCode()
        result = 31 * result + greeting.hashCode()
        result = 31 * result + (avatarData?.contentHashCode() ?: 0)
        return result
    }
}