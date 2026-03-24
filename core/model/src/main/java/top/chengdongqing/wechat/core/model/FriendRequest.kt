package top.chengdongqing.wechat.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class FriendRequest(
    val id: String,
    val userId: String,
    val nickname: String,
    val avatarPath: String?,
    val greeting: String,
    val remark: String?,
    val status: FriendRequestStatus,
    val isFromMe: Boolean,
    val timestamp: Long
)
