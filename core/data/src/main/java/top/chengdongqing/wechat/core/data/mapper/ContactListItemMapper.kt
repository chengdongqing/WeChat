package top.chengdongqing.wechat.core.data.mapper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.ContactItem
import top.chengdongqing.wechat.core.util.getInitial

/** Shared presentation-neutral mapping for contact selection and settings lists. */
fun Contact.toListItem(): ContactItem = ContactItem(
    id = id,
    displayName = displayName,
    nickname = nickname,
    note = note,
    avatarPath = avatarPath,
    isSelf = isSelf,
    initial = displayName.getInitial()
)

@JvmName("toContactListItems")
suspend fun List<Contact>.toListItem(): List<ContactItem> = withContext(Dispatchers.Default) {
    map { it.toListItem() }
}
