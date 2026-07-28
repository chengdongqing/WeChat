package top.chengdongqing.wechat.feature.contacts.data.mapper

import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.model.Contact

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

