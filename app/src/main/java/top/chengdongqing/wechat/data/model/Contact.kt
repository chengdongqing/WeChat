package top.chengdongqing.wechat.data.model

data class Contact(
    val id: String,
    val avatarUrl: String,
    val name: String,      // 备注名
    val gender: Gender,
    val nickname: String  // 昵称
)