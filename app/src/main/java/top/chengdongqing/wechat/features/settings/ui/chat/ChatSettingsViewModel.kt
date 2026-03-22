package top.chengdongqing.wechat.features.settings.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val repository: ChatSettingsRepository,
    private val privateFileManager: PrivateFileManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatSettingsVM"
    }

    val speakerEnabled = repository.speakerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sendButtonEnabled = repository.sendButtonEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val e2eEnabled = repository.e2eEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val chatBackground = repository.chatBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleSpeaker(enabled: Boolean) {
        viewModelScope.launch { repository.toggleSpeaker(!enabled) }
    }

    fun toggleSendButton(enabled: Boolean) {
        viewModelScope.launch { repository.toggleSendButton(enabled) }
    }

    fun toggleE2e(enabled: Boolean) {
        viewModelScope.launch { repository.toggleE2e(enabled) }
    }

    fun setChatBackground(uri: Uri?) {
        viewModelScope.launch {
            try {
                // 保存新背景
                val path = uri?.let { uri ->
                    privateFileManager.saveMedia(
                        messageType = MessageType.Image,
                        sourceUri = uri
                    ).getOrThrow()
                }
                // 删除旧背景
                chatBackground.first()?.let {
                    privateFileManager.deleteFile(it)
                }
                repository.setChatBackground(path)

                context.showToast(if (uri == null) "背景清除成功" else "背景设置成功")
            } catch (e: Exception) {
                Log.e(TAG, "更新背景图片失败", e)
                context.showToast("背景设置失败")
            }
        }
    }
}