package top.chengdongqing.wechat.features.contacts.domain.model

data class IncomingFriendRequest(
    val requestId: String,
    val peerUserId: String,
    val peerNickname: String,
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