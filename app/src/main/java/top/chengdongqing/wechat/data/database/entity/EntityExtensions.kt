package top.chengdongqing.wechat.data.database.entity

import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest

fun ContactEntity.toDomain(): Contact {
    return Contact(
        id = userId,
        nickname = nickname,
        avatarPath = avatarPath,
        signature = signature,
        gender = gender,
        remarkName = remarkName,
        note = note,
        source = source,
        isFromMe = isFromMe,
        addedAt = addedAt
    )
}

fun Contact.toEntity(): ContactEntity {
    return ContactEntity(
        userId = id,
        nickname = nickname,
        avatarPath = avatarPath,
        signature = signature,
        gender = gender,
        remarkName = remarkName,
        note = note,
        addedAt = addedAt ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun FriendRequestEntity.toDomain(): FriendRequest {
    return FriendRequest(
        id = id,
        peerUserId = peerUserId,
        peerNickname = peerNickname,
        peerAvatarPath = peerAvatarPath,
        greetingMessage = greetingMessage,
        remark = remark,
        status = status,
        direction = direction,
        timestamp = createAt
    )
}

@JvmName("toContactDomainList")
fun List<ContactEntity>.toDomain(): List<Contact> = map { it.toDomain() }

@JvmName("toFriendRequestDomainList")
fun List<FriendRequestEntity>.toDomain(): List<FriendRequest> = map { it.toDomain() }