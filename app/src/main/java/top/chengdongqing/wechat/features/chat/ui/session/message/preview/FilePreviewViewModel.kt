package top.chengdongqing.wechat.features.chat.ui.session.message.preview

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
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.file.MediaStoreManager
import top.chengdongqing.wechat.core.util.openFile
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.features.chat.data.mapper.FileContent
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
    private val messageDao: MessageDao,
    private val json: Json,
    private val mediaStoreManager: MediaStoreManager,
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
                val entity =
                    messageDao.getById(messageId) ?: throw IllegalStateException("消息不存在")

                // 解析文件元数据
                val file = json.decodeFromString<FileContent>(entity.content)

                _uiState.update {
                    it.copy(
                        filename = file.filename,
                        fileSize = entity.fileSize ?: 0,
                        mimeType = file.mimeType,
                        localPath = entity.localPath,
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

            val success = mediaStoreManager.saveToDownloads(
                sourceFile = File(_uiState.value.localPath!!),
                filename = _uiState.value.filename
            )

            context.showToast("文件保存${if (success) "成功" else "失败"}")
            _uiState.update {
                it.copy(isSaving = false)
            }
        }
    }
}