package top.chengdongqing.wechat.feature.discovery.moments

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Moment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String? = null,
    val content: String = "",
    val images: List<String> = emptyList(),
    val video: MomentVideo? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val likes: List<MomentLike> = emptyList(),
    val comments: List<MomentComment> = emptyList()
)

@Immutable
@Serializable
data class MomentVideo(
    val path: String,
    val width: Int,
    val height: Int,
    val duration: Long,
    val thumbnailPath: String? = null
)

@Immutable
@Serializable
data class MomentLike(val userId: String, val userName: String)

@Immutable
@Serializable
data class MomentComment(
    val id: String,
    val userId: String,
    val userName: String,
    val content: String,
    val createdAt: Long
)

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

@Serializable
data class MomentsEnvelope(
    val senderId: String,
    val state: MomentsState,
    val sentAt: Long = System.currentTimeMillis()
)
