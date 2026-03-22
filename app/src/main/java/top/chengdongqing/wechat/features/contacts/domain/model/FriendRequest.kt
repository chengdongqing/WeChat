package top.chengdongqing.wechat.features.contacts.domain.model

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.model.FriendRequestStatus

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