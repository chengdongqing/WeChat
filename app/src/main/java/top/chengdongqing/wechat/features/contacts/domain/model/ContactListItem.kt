package top.chengdongqing.wechat.features.contacts.domain.model

/**
 * 联系人列表项
 */
data class ContactListItem(
    val id: String,
    val name: String,
    val note: String?,
    val avatarPath: String?,
    val isSelf: Boolean,
    val initial: Char
)