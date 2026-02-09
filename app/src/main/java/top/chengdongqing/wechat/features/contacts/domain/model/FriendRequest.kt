package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.database.entity.RequestStatus

data class FriendRequest(
    val requestId: String,
    val fromUserId: String,
    val fromNickname: String,
    val fromAvatarPath: String?,
    val greetingMessage: String,
    val remark: String?,
    val status: RequestStatus,
    val timestamp: Long
)