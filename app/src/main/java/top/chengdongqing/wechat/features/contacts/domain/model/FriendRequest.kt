package top.chengdongqing.wechat.features.contacts.domain.model

import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromNickname: String,
    val fromAvatarPath: String?,
    val greetingMessage: String,
    val remark: String?,
    val status: RequestStatus,
    val direction: RequestDirection,
    val timestamp: Long
)