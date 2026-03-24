package top.chengdongqing.wechat.feature.contacts.data.mapper

import top.chengdongqing.wechat.core.database.entity.FriendRequestEntity
import top.chengdongqing.wechat.core.model.FriendRequest

fun FriendRequestEntity.toDomain(): FriendRequest {
    return FriendRequest(
        id = id,
        userId = userId,
        nickname = nickname,
        avatarPath = avatarPath,
        greeting = greeting,
        remark = remark,
        status = status,
        isFromMe = isFromMe,
        timestamp = audit.createdAt
    )
}

@JvmName("toFriendRequestDomainList")
fun List<FriendRequestEntity>.toDomain(): List<FriendRequest> = map { it.toDomain() }