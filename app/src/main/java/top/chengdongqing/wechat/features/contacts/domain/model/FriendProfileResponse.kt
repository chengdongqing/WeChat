package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.model.Gender

data class FriendProfileResponse(
    val userId: String,
    val nickname: String,
    val signature: String?,
    val gender: Gender?,
    val avatarData: ByteArray?
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