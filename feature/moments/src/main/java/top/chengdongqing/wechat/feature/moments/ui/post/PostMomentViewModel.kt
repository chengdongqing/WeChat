package top.chengdongqing.wechat.feature.moments.ui.post

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.feature.moments.data.MomentsRepository
import javax.inject.Inject

@Immutable
sealed interface PostMomentMedia {
    data object Empty : PostMomentMedia
    data class Images(val uris: List<Uri>) : PostMomentMedia
    data class EditingVideo(
        val source: Uri,
        val previous: PostMomentMedia
    ) : PostMomentMedia

    data class Video(val uri: Uri) : PostMomentMedia
}

@Immutable
data class PostMomentUiState(
    val content: String = "",
    val media: PostMomentMedia = PostMomentMedia.Empty,
    val isPublishing: Boolean = false
) {
    val canPublish: Boolean
        get() = !isPublishing && (content.isNotBlank() || media is PostMomentMedia.Images || media is PostMomentMedia.Video)

    val remainingImageCount: Int
        get() = MAX_IMAGE_COUNT - ((media as? PostMomentMedia.Images)?.uris?.size ?: 0)

    companion object {
        const val MAX_CONTENT_LENGTH = 2_000
        const val MAX_IMAGE_COUNT = 9
    }
}

sealed interface PostMomentEvent {
    data object Published : PostMomentEvent
    data class Message(val text: String) : PostMomentEvent
}

@HiltViewModel
class PostMomentViewModel @Inject constructor(
    private val repository: MomentsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(restoreState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PostMomentEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun updateContent(value: String) {
        updateState { copy(content = value.take(PostMomentUiState.MAX_CONTENT_LENGTH)) }
    }

    fun addCapturedMedia(uri: Uri, isImage: Boolean) {
        if (isImage) addImages(listOf(uri)) else editVideo(uri)
    }

    fun addSelectedMedia(items: List<MediaItem>) {
        if (items.isEmpty()) return
        when {
            items.all(MediaItem::isImage) -> addImages(items.map(MediaItem::uri))
            items.size == 1 && items.first().isVideo -> editVideo(items.first().uri)
            else -> _events.tryEmit(PostMomentEvent.Message("图片和视频不能同时选择"))
        }
    }

    fun removeImage(index: Int) {
        val images = (_uiState.value.media as? PostMomentMedia.Images)?.uris ?: return
        if (index !in images.indices) return
        val updated = images.toMutableList().apply { removeAt(index) }
        updateMedia(if (updated.isEmpty()) PostMomentMedia.Empty else PostMomentMedia.Images(updated))
    }

    fun removeVideo() = updateMedia(PostMomentMedia.Empty)

    fun cancelVideoEditing() {
        val editing = _uiState.value.media as? PostMomentMedia.EditingVideo ?: return
        updateMedia(editing.previous)
    }

    fun completeVideoEditing(uri: Uri) = updateMedia(PostMomentMedia.Video(uri))

    fun publish() {
        val snapshot = _uiState.value
        if (!snapshot.canPublish) return
        _uiState.update { it.copy(isPublishing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (val media = snapshot.media) {
                    PostMomentMedia.Empty -> repository.publish(snapshot.content, emptyList())
                    is PostMomentMedia.Images -> repository.publish(snapshot.content, media.uris)
                    is PostMomentMedia.Video -> repository.publishVideo(snapshot.content, media.uri)
                    is PostMomentMedia.EditingVideo -> error("Video editing is not complete")
                }.also { success -> check(success) { "媒体文件保存失败" } }
            }.onSuccess {
                clearDraft()
                _events.emit(PostMomentEvent.Published)
            }.onFailure {
                _uiState.update { it.copy(isPublishing = false) }
                _events.emit(PostMomentEvent.Message(it.message ?: "发表失败，请重试"))
            }
        }
    }

    private fun addImages(newUris: List<Uri>) {
        val current = (_uiState.value.media as? PostMomentMedia.Images)?.uris.orEmpty()
        val updated = (current + newUris).distinct().take(PostMomentUiState.MAX_IMAGE_COUNT)
        updateMedia(PostMomentMedia.Images(updated))
    }

    private fun editVideo(uri: Uri) {
        val previous = when (val media = _uiState.value.media) {
            is PostMomentMedia.EditingVideo -> media.previous
            else -> media
        }
        updateMedia(PostMomentMedia.EditingVideo(uri, previous))
    }

    private fun updateMedia(media: PostMomentMedia) = updateState { copy(media = media) }

    private fun updateState(transform: PostMomentUiState.() -> PostMomentUiState) {
        _uiState.update { state -> state.transform().also(::saveState) }
    }

    private fun restoreState(): PostMomentUiState {
        val content = savedStateHandle[KEY_CONTENT] ?: ""
        val images = savedStateHandle.get<List<Uri>>(KEY_IMAGES).orEmpty()
        val video = savedStateHandle.get<Uri>(KEY_VIDEO)
        return PostMomentUiState(
            content = content,
            media = when {
                video != null -> PostMomentMedia.Video(video)
                images.isNotEmpty() -> PostMomentMedia.Images(images)
                else -> PostMomentMedia.Empty
            }
        )
    }

    private fun saveState(state: PostMomentUiState) {
        val persistedMedia = (state.media as? PostMomentMedia.EditingVideo)?.previous ?: state.media
        savedStateHandle[KEY_CONTENT] = state.content
        savedStateHandle[KEY_IMAGES] = (persistedMedia as? PostMomentMedia.Images)?.uris.orEmpty()
        savedStateHandle[KEY_VIDEO] = (persistedMedia as? PostMomentMedia.Video)?.uri
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(KEY_CONTENT)
        savedStateHandle.remove<List<Uri>>(KEY_IMAGES)
        savedStateHandle.remove<Uri>(KEY_VIDEO)
    }

    private companion object {
        const val KEY_CONTENT = "post_moment_content"
        const val KEY_IMAGES = "post_moment_images"
        const val KEY_VIDEO = "post_moment_video"
    }
}
