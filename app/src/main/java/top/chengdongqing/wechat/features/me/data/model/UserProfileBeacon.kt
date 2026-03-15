package top.chengdongqing.wechat.features.me.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.features.me.domain.model.Gender

/**
 * 通过 P2P 传输的用户资料
 */
@Serializable
data class UserProfileBeacon(
    val userId: String,
    val nickname: String,
    val signature: String? = null,
    val gender: Gender? = null,
    val avatarSize: Int = 0, // 头像大小（字节）
    val avatarUrl: String? = null,
    val publicKey: String    // 公钥
)