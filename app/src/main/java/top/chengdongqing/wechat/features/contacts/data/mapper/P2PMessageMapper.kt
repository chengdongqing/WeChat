package top.chengdongqing.wechat.features.contacts.data.mapper

import top.chengdongqing.wechat.data.network.model.FriendProtocol
import top.chengdongqing.wechat.data.network.model.FriendRequestResult
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest

fun FriendProtocol.FriendRequest.toDomain(avatarData: ByteArray?): IncomingFriendRequest {
    return IncomingFriendRequest(
        requestId = requestId,
        userId = userId,
        nickname = nickname,
        publicKey = publicKey,
        greeting = greeting,
        avatarData = avatarData,
        timestamp = timestamp
    )
}

fun FriendProtocol.FriendResponse.toDomain(): FriendRequestResponse {
    return FriendRequestResponse(
        requestId = requestId,
        accepted = result == FriendRequestResult.Accepted
    )
}