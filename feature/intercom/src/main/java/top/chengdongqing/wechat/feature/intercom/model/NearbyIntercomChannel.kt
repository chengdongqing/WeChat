package top.chengdongqing.wechat.feature.intercom.model

data class NearbyIntercomChannel(
    val id: String,
    val memberCount: Int,
    val speakingCount: Int,
    val lastSeenAt: Long
)