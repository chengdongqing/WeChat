package top.chengdongqing.wechat.feature.intercom.model

data class NearbyIntercomChannel(
    val id: String,
    val name: String,
    val memberCount: Int,
    val speakingCount: Int,
    val lastSeenAt: Long
)

data class IntercomMember(
    val id: String,
    val nickname: String,
    val isSpeaking: Boolean,
    val isMe: Boolean
)

data class IntercomRoomState(
    val channelId: String = "",
    val channelName: String = "",
    val members: List<IntercomMember> = emptyList()
) {
    val speakers: List<IntercomMember>
        get() = members.filter(IntercomMember::isSpeaking)
}
