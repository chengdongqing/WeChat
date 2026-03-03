package top.chengdongqing.wechat.features.contacts.data.mapper

import top.chengdongqing.wechat.core.util.PinyinHelper.getInitial
import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactListItem
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
    addedAt = audit.createdAt
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    remarkName = remarkName,
    note = note,
    isBlocked = isBlocked
)

@JvmName("toContactDomainList")
fun List<ContactEntity>.toDomain(): List<Contact> = map { it.toDomain() }

fun Contact.toListItem(): ContactListItem = ContactListItem(
    id = id,
    name = displayName,
    note = note,
    avatarPath = avatarPath,
    isSelf = isSelf,
    initial = displayName.getInitial()
)

@JvmName("toContactList")
fun List<Contact>.toListItem(): List<ContactListItem> = map { it.toListItem() }

fun UserProfile.toContact(): Contact = Contact(
    id = id,
    nickname = nickname,
    avatarPath = avatarPath,
    signature = signature,
    gender = gender,
    relation = ContactRelation.Myself
)