package top.chengdongqing.wechat.features.contacts.data.mapper

import top.chengdongqing.wechat.data.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest

fun FriendRequestEntity.toDomain(): FriendRequest {
    return FriendRequest(
        id = id,
        peerUserId = peerId,
        peerNickname = peerName,
        peerAvatarPath = peerAvatarPath,
        greetingMessage = greetingMessage,
        remark = remark,
        status = status,
        direction = direction,
        timestamp = audit.createdAt
    )
}

@JvmName("toFriendRequestDomainList")
fun List<FriendRequestEntity>.toDomain(): List<FriendRequest> = map { it.toDomain() }