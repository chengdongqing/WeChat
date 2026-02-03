package top.chengdongqing.wechat.features.contacts.model

import top.chengdongqing.wechat.data.model.Gender

data class Contact(
    val id: String,
    val avatarUrl: String,
    val remarkName: String,
    val gender: Gender? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val momentPhotos: List<Int> = emptyList()
)