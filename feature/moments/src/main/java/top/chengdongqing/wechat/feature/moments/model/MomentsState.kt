package top.chengdongqing.wechat.feature.moments.model

import kotlinx.serialization.Serializable

@Serializable
data class MomentsState(
    val moments: List<Moment> = emptyList(),
    val covers: Map<String, MomentCover> = emptyMap(),
    // 兼容旧版本数据，读取后会迁移到 covers。
    val coverPath: String? = null,
    val coverUpdatedAt: Long = 0,
    val deletedMoments: Map<String, Long> = emptyMap(),
    val version: Long = 0
)

@Serializable
data class MomentCover(
    val path: String,
    val updatedAt: Long
)

fun MomentsState.coverFor(userId: String): String? = covers[userId]?.path
