package top.chengdongqing.wechat.data.call.model

data class Call(
    val id: String,
    val participantId: String,
    val participantName: String,
    val type: CallType,
    val state: CallState,
    val isIncoming: Boolean
)