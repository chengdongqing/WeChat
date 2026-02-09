package top.chengdongqing.wechat.data.model

import kotlinx.serialization.Serializable

/**
 * 通过 P2P 传输的用户资料
 */
@Serializable
data class UserProfileTransfer(
    val userId: String,
    val nickname: String,
    val signature: String? = null,
    val gender: Int = -1,
    val avatarSize: Int = 0 // 头像大小（字节）
)