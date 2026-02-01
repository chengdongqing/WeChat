package top.chengdongqing.wechat.data.model

data class Contact(
    val id: String,
    val avatarUrl: String,
    val name: String,
    val gender: Gender = Gender.Unknown,
    val nickname: String = "",
    val tags: List<String> = emptyList(),
    val remark: String = "",
    val momentPhotos: List<Int> = emptyList()
)