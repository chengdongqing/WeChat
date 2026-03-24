package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.model.Gender
import top.chengdongqing.wechat.core.model.UserProfile

@Serializable
data class ProfileBeacon(
    val userId: String,
    val nickname: String,
    val signature: String? = null,
    val gender: Gender? = null,
    val avatarSize: Int = 0,
    val avatarUrl: String? = null,
    val publicKey: String
)

fun UserProfile.toBeacon(avatarBytes: ByteArray?) = ProfileBeacon(
    userId = id,
    nickname = nickname,
    signature = signature,
    gender = gender,
    publicKey = publicKey,
    avatarSize = avatarBytes?.size ?: 0
)
