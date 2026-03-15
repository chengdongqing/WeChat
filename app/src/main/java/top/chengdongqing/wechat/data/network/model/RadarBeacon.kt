package top.chengdongqing.wechat.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RadarBeacon(
    val userId: String,
    val nickname: String,
    val avatarUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)