package top.chengdongqing.wechat.features.contacts.data.mapper

import top.chengdongqing.wechat.data.database.entity.ContactEntity
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

fun ContactEntity.toDomain(): Contact {
    return Contact(
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
}

fun Contact.toEntity(): ContactEntity {
    return ContactEntity(
        id = id,
        nickname = nickname,
        avatarPath = avatarPath,
        signature = signature,
        gender = gender,
        remarkName = remarkName,
        note = note,
        isBlocked = isBlocked
    )
}

@JvmName("toContactDomainList")
fun List<ContactEntity>.toDomain(): List<Contact> = map { it.toDomain() }