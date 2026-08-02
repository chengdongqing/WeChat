package top.chengdongqing.wechat.feature.moments.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.feature.moments.model.Moment
import top.chengdongqing.wechat.feature.moments.model.MomentComment
import top.chengdongqing.wechat.feature.moments.model.MomentCover
import top.chengdongqing.wechat.feature.moments.model.MomentLike
import top.chengdongqing.wechat.feature.moments.model.MomentVideo
import top.chengdongqing.wechat.feature.moments.model.MomentsState
import java.io.File
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MomentsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val preferences = context.getSharedPreferences("moments", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadAndMigrate())
    val state = _state.asStateFlow()
    var onLocalChange: ((MomentsState) -> Unit)? = null

    fun publish(content: String, imageUris: List<Uri>) {
        val profile = profileRepository.requireProfile()
        val images = imageUris.mapNotNull { persistImage(it, "post") }
        mutate {
            copy(
                moments = listOf(
                    Moment(
                        id = UUID.randomUUID().toString(),
                        authorId = profile.id,
                        authorName = profile.nickname,
                        authorAvatar = profile.avatarPath,
                        content = content.trim(),
                        images = images,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                ) + moments
            )
        }
    }

    fun publishVideo(content: String, videoUri: Uri) {
        val profile = profileRepository.requireProfile()
        val path = persistMedia(videoUri, "video", "mp4") ?: return
        val metadata = android.media.MediaMetadataRetriever()
        val video = runCatching {
            metadata.setDataSource(path)
            MomentVideo(
                path = path,
                width = metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 1,
                height = metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 1,
                duration = metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0
            )
        }.also { metadata.release() }.getOrNull() ?: return
        mutate {
            copy(
                moments = listOf(
                    Moment(
                        id = UUID.randomUUID().toString(),
                        authorId = profile.id,
                        authorName = profile.nickname,
                        authorAvatar = profile.avatarPath,
                        content = content.trim(),
                        video = video,
                        createdAt = System.currentTimeMillis()
                    )
                ) + moments
            )
        }
    }

    fun toggleLike(momentId: String) {
        val profile = profileRepository.requireProfile()
        mutate {
            copy(moments = moments.map { moment ->
                if (moment.id != momentId) moment else {
                    val liked = moment.likes.any { it.userId == profile.id }
                    moment.copy(
                        likes = if (liked) moment.likes.filterNot { it.userId == profile.id }
                        else moment.likes + MomentLike(profile.id, profile.nickname),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            })
        }
    }

    fun comment(momentId: String, text: String) {
        if (text.isBlank()) return
        val profile = profileRepository.requireProfile()
        mutate {
            copy(moments = moments.map { moment ->
                if (moment.id != momentId) moment else moment.copy(
                    comments = moment.comments + MomentComment(
                        UUID.randomUUID().toString(),
                        profile.id,
                        profile.nickname,
                        text.trim(),
                        System.currentTimeMillis()
                    ),
                    updatedAt = System.currentTimeMillis()
                )
            })
        }
    }

    fun delete(momentId: String) {
        val myId = profileRepository.requireUserId()
        mutate {
            val canDelete = moments.any { it.id == momentId && it.authorId == myId }
            if (!canDelete) this else copy(
                moments = moments.filterNot { it.id == momentId },
                deletedMoments = deletedMoments + (momentId to System.currentTimeMillis())
            )
        }
    }

    fun setCover(uri: Uri) {
        val path = persistImage(uri, "cover") ?: return
        setCoverPath(path)
    }

    fun setCoverFromUrl(url: String): Boolean {
        val path = runCatching {
            val directory = File(context.filesDir, "moments").apply { mkdirs() }
            val target = File(directory, "cover_${UUID.randomUUID()}.jpg")
            URL(url).openStream().use { input -> target.outputStream().use(input::copyTo) }
            target.absolutePath
        }.getOrNull() ?: return false
        setCoverPath(path)
        return true
    }

    private fun setCoverPath(path: String) {
        val userId = profileRepository.requireUserId()
        mutate {
            copy(
                covers = covers + (
                    userId to MomentCover(
                        path = path,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    fun mergeRemote(remote: MomentsState, senderId: String) {
        val local = _state.value
        val normalizedRemote = remote.withLegacyCoverOwner(senderId)
        val tombstones = (local.deletedMoments.keys + remote.deletedMoments.keys).associateWith { id ->
            maxOf(local.deletedMoments[id] ?: 0, remote.deletedMoments[id] ?: 0)
        }
        val merged = (local.moments + normalizedRemote.moments)
            .groupBy { it.id }
            .mapNotNull { (id, versions) ->
                val newest = versions.maxBy { it.updatedAt }
                if ((tombstones[id] ?: 0) >= newest.updatedAt) null else newest
            }
            .sortedByDescending { it.createdAt }
        val mergedCovers = (local.covers.keys + normalizedRemote.covers.keys).associateWith { userId ->
            listOfNotNull(local.covers[userId], normalizedRemote.covers[userId])
                .maxBy { it.updatedAt }
        }
        val next = local.copy(
            moments = merged,
            covers = mergedCovers,
            coverPath = null,
            coverUpdatedAt = 0,
            deletedMoments = tombstones,
            version = maxOf(local.version, normalizedRemote.version)
        )
        if (next != local) save(next)
    }

    private fun mutate(block: MomentsState.() -> MomentsState) {
        val next = _state.value.block().copy(version = System.currentTimeMillis())
        save(next)
        onLocalChange?.invoke(next)
    }

    private fun persistImage(uri: Uri, prefix: String): String? = runCatching {
        persistMedia(uri, prefix, "jpg")
    }.getOrNull()

    private fun persistMedia(uri: Uri, prefix: String, extension: String): String? = runCatching {
        val directory = File(context.filesDir, "moments").apply { mkdirs() }
        val target = File(directory, "${prefix}_${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use(input::copyTo)
        } ?: return null
        target.absolutePath
    }.getOrNull()

    private fun loadAndMigrate(): MomentsState {
        val loaded = preferences.getString(KEY_STATE, null)
        ?.let { runCatching { json.decodeFromString<MomentsState>(it) }.getOrNull() }
        ?: MomentsState()
        val migrated = loaded.withLegacyCoverOwner(profileRepository.requireUserId())
        if (migrated != loaded) {
            preferences.edit().putString(KEY_STATE, json.encodeToString(migrated)).apply()
        }
        return migrated
    }

    private fun MomentsState.withLegacyCoverOwner(ownerId: String): MomentsState {
        val legacyPath = coverPath ?: return this
        val legacyCover = MomentCover(legacyPath, coverUpdatedAt)
        val current = covers[ownerId]
        return copy(
            covers = if (current == null || legacyCover.updatedAt > current.updatedAt) {
                covers + (ownerId to legacyCover)
            } else {
                covers
            },
            coverPath = null,
            coverUpdatedAt = 0
        )
    }

    private fun save(value: MomentsState) {
        _state.value = value
        preferences.edit().putString(KEY_STATE, json.encodeToString(value)).apply()
    }

    private companion object { const val KEY_STATE = "state" }
}
