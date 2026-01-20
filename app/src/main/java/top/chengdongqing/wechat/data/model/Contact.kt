package top.chengdongqing.wechat.data.model

data class Contact(
    val id: String,
    val name: String,
    val avatar: Int,
    val initial: Char // 首字母
)