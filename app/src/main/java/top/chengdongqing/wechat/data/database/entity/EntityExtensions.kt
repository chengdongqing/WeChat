package top.chengdongqing.wechat.data.database.entity

import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest

/**
 * Entity 转 Domain 扩展函数
 */

fun ContactEntity.toDomain(): Contact {
    return Contact(
        id = userId,
        nickname = nickname,
        avatarPath = avatarPath,
        signature = signature,
        gender = gender,
        remarkName = remarkName,
        note = note
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