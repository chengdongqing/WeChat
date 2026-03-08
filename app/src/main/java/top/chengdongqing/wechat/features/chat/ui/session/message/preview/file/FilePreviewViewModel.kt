package top.chengdongqing.wechat.features.chat.ui.session.message.preview.file

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.file.PublicFileManager
import top.chengdongqing.wechat.core.util.openFile
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import java.io.File

data class FilePreviewUiState(
    val filename: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "*/*",
    val localPath: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val fileExists: Boolean
        get() = localPath != null && File(localPath).exists()
}

@HiltViewModel(assistedFactory = FilePreviewViewModel.Factory::class)
class FilePreviewViewModel @AssistedInject constructor(
    @Assisted private val messageId: String,
    private val messageRepository: MessageRepository,
    private val publicFileManager: PublicFileManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(messageId: String): FilePreviewViewModel
    }

    companion object {
        const val TAG = "FilePreviewViewModel"
    }

    private val _uiState = MutableStateFlow(FilePreviewUiState())
    val uiState: StateFlow<FilePreviewUiState> = _uiState.asStateFlow()

    init {
        loadFileInfo()
    }

    private fun loadFileInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val message = messageRepository.getMessage(messageId)
                    ?: throw IllegalStateException("消息不存在")
                val file = message.content as MessageContent.File

                _uiState.update {
                    it.copy(
                        filename = file.filename,
                        fileSize = file.size,
                        mimeType = file.mimeType,
                        localPath = file.localPath,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "文件信息加载失败", e)
                context.showToast("文件信息加载失败")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 打开文件
     */
    fun openFile() {
        val state = _uiState.value
        val path = state.localPath ?: return
        val file = File(path)

        try {
            context.openFile(file, state.mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败", e)
            context.showToast("没有找到可以打开此文件的应用")
        }
    }

    /**
     * 保存文件
     */
    fun saveFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val res = publicFileManager.saveMedia(
                messageType = MessageType.File,
                sourceFile = File(_uiState.value.localPath!!),
                filename = _uiState.value.filename
            )

            context.showToast("文件保存${if (res != null) "成功" else "失败"}")
            _uiState.update {
                it.copy(isSaving = false)
            }
        }
    }
}