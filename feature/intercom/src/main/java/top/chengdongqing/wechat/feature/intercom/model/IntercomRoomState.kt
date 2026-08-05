package top.chengdongqing.wechat.feature.intercom.model

data class IntercomRoomState(
    val channelId: String = "",
    val channelName: String = "",
    val members: List<IntercomMember> = emptyList()
) {
    val speakers: List<IntercomMember>
        get() = members.filter(IntercomMember::isSpeaking)
}
