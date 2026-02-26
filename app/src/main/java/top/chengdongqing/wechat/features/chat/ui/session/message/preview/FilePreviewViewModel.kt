package top.chengdongqing.wechat.features.chat.ui.session.message.preview

import android.content.Context
import android.os.Environment
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.openFile
import top.chengdongqing.wechat.data.database.dao.MessageDao
import top.chengdongqing.wechat.features.chat.data.mapper.FileContent
import java.io.File

data class FilePreviewUiState(
    val filename: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "*/*",
    val localPath: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    val fileExists: Boolean
        get() = localPath != null && File(localPath).exists()
}

@HiltViewModel(assistedFactory = FilePreviewViewModel.Factory::class)
class FilePreviewViewModel @AssistedInject constructor(
    @Assisted private val messageId: String,
    private val messageDao: MessageDao,
    private val json: Json,
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
                val entity = messageDao.getById(messageId)
                if (entity == null) {
                    _uiState.update { it.copy(isLoading = false, error = "消息不存在") }
                    return@launch
                }

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
                Log.e(TAG, "加载文件信息失败", e)
                _uiState.update { it.copy(isLoading = false, error = "加载失败") }
            }
        }
    }

    /**
     * 用系统应用打开文件
     */
    fun openFile() {
        val state = _uiState.value
        val path = state.localPath ?: return
        val file = File(path)
        if (!file.exists()) {
            _uiState.update { it.copy(error = "文件不存在") }
            return
        }

        try {
            context.openFile(file, state.mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败", e)
            _uiState.update { it.copy(error = "没有找到可以打开此文件的应用") }
        }
    }

    /**
     * 保存到公共下载目录
     */
    fun saveToDownloads() {
        val path = _uiState.value.localPath ?: return
        val sourceFile = File(path)
        if (!sourceFile.exists()) {
            _uiState.update { it.copy(error = "文件不存在") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val appName = context.getString(R.string.app_name)
                    val relativePath = "$downloadsDir/$appName"
                    val targetFile = File(relativePath, _uiState.value.filename)

                    // 同名文件处理
                    val finalFile = if (targetFile.exists()) {
                        val name = targetFile.nameWithoutExtension
                        val ext = targetFile.extension
                        val timestamp = System.currentTimeMillis()
                        File(relativePath, "${name}_$timestamp.$ext")
                    } else {
                        targetFile
                    }

                    sourceFile.copyTo(finalFile, overwrite = true)
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "保存文件失败", e)
                _uiState.update { it.copy(isSaving = false, error = "保存失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}