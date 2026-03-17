package top.chengdongqing.wechat.features.contacts.data.mapper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.util.getInitial
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactItem
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.me.domain.model.UserProfile

fun ContactEntity.toDomain(): Contact = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    remarkName = remarkName,
    note = note,
    source = source,
    isBlocked = isBlocked,
    isFromMe = isFromMe,
    publicKey = publicKey,
    addedAt = audit.createdAt,
    version = version
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    remarkName = remarkName,
    note = note,
    isBlocked = isBlocked,
    source = source,
    isFromMe = isFromMe,
    publicKey = publicKey,
    version = version
)

@JvmName("toContactDomainList")
fun List<ContactEntity>.toDomain(): List<Contact> = map { it.toDomain() }

fun Contact.toListItem(): ContactItem = ContactItem(
    id = id,
    name = displayName,
    note = note,
    avatarPath = avatarPath,
    isSelf = isSelf,
    initial = displayName.getInitial()
)

@JvmName("toContactList")
suspend fun List<Contact>.toListItem(): List<ContactItem> =
    withContext(Dispatchers.Default) {
        map { it.toListItem() }
    }

fun UserProfile.toContact(): Contact = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    relation = ContactRelation.Myself
)