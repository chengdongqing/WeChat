package top.chengdongqing.wechat.feature.intercom.model

data class IntercomMember(
    val id: String,
    val nickname: String,
    val isSpeaking: Boolean,
    val isMe: Boolean
)