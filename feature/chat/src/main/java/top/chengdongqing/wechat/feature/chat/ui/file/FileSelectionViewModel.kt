package top.chengdongqing.wechat.feature.chat.ui.file

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.database.dao.MessageDao
import top.chengdongqing.wechat.core.file.FileMetadata
import top.chengdongqing.wechat.core.media.model.MediaType
import top.chengdongqing.wechat.core.media.repository.LocalMediaRepositoryImpl
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.feature.chat.data.mapper.FileContent
import java.io.File

data class FileSelectionUiState(
    val chatFiles: List<FileMetadata> = emptyList(),
    val mediaFiles: List<FileMetadata> = emptyList(),
    val loadingChat: Boolean = true,
    val loadingMedia: Boolean = true
)

@HiltViewModel
class FileSelectionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val messageDao: MessageDao
) : ViewModel() {
    private val mediaRepository = LocalMediaRepositoryImpl(context)
    private val _uiState = MutableStateFlow(FileSelectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val files = messageDao.getAllByType(MessageType.File).mapNotNull { message ->
                val path = message.localPath ?: return@mapNotNull null
                val file = File(path)
                if (!file.isFile) return@mapNotNull null
                val content =
                    runCatching { Json.decodeFromString<FileContent>(message.content) }.getOrNull()
                val filename = content?.filename?.takeIf(String::isNotBlank) ?: file.name
                val extension = filename.substringAfterLast('.', "").lowercase()
                FileMetadata(
                    uri = Uri.fromFile(file).buildUpon().fragment(filename).build(),
                    filename = filename,
                    size = message.fileSize ?: file.length(),
                    mimeType = content?.mimeType?.takeIf(String::isNotBlank)
                        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                        ?: "application/octet-stream"
                )
            }
            _uiState.update { it.copy(chatFiles = files, loadingChat = false) }
        }
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMedia = true) }
            val media =
                mediaRepository.loadMediaList(arrayOf(MediaType.Image, MediaType.Video)).map {
                    FileMetadata(
                        uri = it.uri,
                        filename = it.filename,
                        size = it.size,
                        mimeType = it.mimeType,
                        width = it.width,
                        height = it.height,
                        duration = it.duration,
                        isMedia = true
                    )
                }
            _uiState.update { it.copy(mediaFiles = media, loadingMedia = false) }
        }
    }
}
