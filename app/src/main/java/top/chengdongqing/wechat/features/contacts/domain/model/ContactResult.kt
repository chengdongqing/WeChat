package top.chengdongqing.wechat.features.contacts.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactResult(
    val id: String,
    val nickname: String,
    val avatarPath: String,
) : Parcelable

fun List<ContactItem>.toResult() = map { contact ->
    ContactResult(
        id = contact.id,
        nickname = contact.nickname,
        avatarPath = contact.avatarPath!!
    )
}

fun Contact.toResult() = ContactResult(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath!!
)