package top.chengdongqing.wechat.data.model

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean
)