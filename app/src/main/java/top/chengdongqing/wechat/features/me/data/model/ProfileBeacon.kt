package top.chengdongqing.wechat.features.me.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.model.UserProfile

@Serializable
data class ProfileBeacon(
    val userId: String,
    val nickname: String,
    val signature: String? = null,
    val gender: Gender? = null,
    val avatarSize: Int = 0, // 头像大小（字节）
    val avatarUrl: String? = null,
    val publicKey: String    // 公钥
)

fun UserProfile.toBeacon(avatarBytes: ByteArray?) = ProfileBeacon(
    userId = id,
    nickname = nickname,
    signature = signature,
    gender = gender,
    publicKey = publicKey,
    avatarSize = avatarBytes?.size ?: 0
)