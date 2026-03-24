package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RadarBeacon(
    val userId: String,
    val nickname: String,
    val avatarUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
