package top.chengdongqing.wechat.data.model

data class Contact(
    val id: String,
    val avatarUrl: String,
    val remarkName: String,
    val gender: Gender = Gender.Unknown,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val momentPhotos: List<Int> = emptyList()
)