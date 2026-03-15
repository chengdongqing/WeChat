package top.chengdongqing.wechat.features.contacts.data.mapper

import top.chengdongqing.wechat.data.network.model.P2PMessage
import top.chengdongqing.wechat.data.network.model.RequestAction
import top.chengdongqing.wechat.features.contacts.domain.model.FriendProfileResponse
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequestResponse
import top.chengdongqing.wechat.features.contacts.domain.model.IncomingFriendRequest

fun P2PMessage.FriendRequest.toDomain(avatarData: ByteArray?): IncomingFriendRequest {
    return IncomingFriendRequest(
        requestId = requestId,
        peerUserId = peerUserId,
        peerNickname = peerNickname,
        peerPublicKey = peerPublicKey,
        greetingMessage = greetingMessage,
        avatarData = avatarData,
        timestamp = timestamp
    )
}

fun P2PMessage.FriendRequestResponse.toDomain(): FriendRequestResponse {
    return FriendRequestResponse(
        requestId = requestId,
        accepted = action == RequestAction.ACCEPT
    )
}

fun P2PMessage.AutoAddResponse.toDomain(avatarData: ByteArray?): FriendProfileResponse {
    return FriendProfileResponse(
        userId = userId,
        nickname = nickname,
        signature = signature,
        gender = gender,
        avatarData = avatarData,
        publicKey = publicKey
    )
}

fun P2PMessage.FullProfileResponse.toDomain(avatarData: ByteArray?): FriendProfileResponse {
    return FriendProfileResponse(
        userId = userId,
        nickname = nickname,
        signature = signature,
        gender = gender,
        avatarData = avatarData,
        publicKey = publicKey
    )
}