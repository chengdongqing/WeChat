package top.chengdongqing.wechat.feature.moments.model

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
