package top.chengdongqing.wechat.features.contacts.domain.model

import androidx.compose.runtime.Immutable

/**
 * 联系人列表项
 */
@Immutable
data class ContactItem(
    val id: String,
    val displayName: String,
    val nickname: String,
    val note: String? = null,
    val avatarPath: String? = null,
    val isSelf: Boolean = false,
    val initial: Char = '#'
)