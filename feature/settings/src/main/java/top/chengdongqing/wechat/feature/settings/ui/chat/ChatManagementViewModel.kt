package top.chengdongqing.wechat.feature.settings.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.util.showToast
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import javax.inject.Inject

@HiltViewModel
class ChatManagementViewModel @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatManagementVM"
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            runCatching {
                chatSessionRepository.deleteAllSessions()
            }.onSuccess {
                context.showToast("已清空")
            }.onFailure {
                context.showToast("操作失败")
                Log.e(TAG, "清空聊天记录失败", it)
            }
        }
    }
}