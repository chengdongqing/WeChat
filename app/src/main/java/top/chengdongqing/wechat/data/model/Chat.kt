package top.chengdongqing.wechat.data.model

data class Chat(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val time: String,
    val avatarRes: Int,
    val unreadCount: Int = 0
)