package top.chengdongqing.wechat.features.chat.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.settings.domain.repository.ConnectionSettingsRepository
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    private val connectionSettingsRepository: ConnectionSettingsRepository,
    val activeSessionManager: ActiveSessionManager
) : ViewModel() {

    val chats = chatSessionRepository
        .observeAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val connectionMode = connectionSettingsRepository.connectionMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionMode.WiFiLan
    )

    /**
     * 标为已读/未读
     */
    fun toggleReadStatus(sessionId: String, hasUnread: Boolean) {
        viewModelScope.launch {
            if (hasUnread) {
                chatSessionRepository.clearUnreadCount(sessionId)
            } else {
                chatSessionRepository.markAsUnread(sessionId)
            }
        }
    }

    /**
     * 聊天置顶
     */
    fun stickToTop(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatSessionRepository.togglePin(sessionId, !isPinned)
        }
    }

    /**
     * 隐藏聊天
     */
    fun hideChat(sessionId: String) {
        viewModelScope.launch {
            chatSessionRepository.hideSession(sessionId)
        }
    }

    /**
     * 删除聊天
     */
    fun deleteChat(sessionId: String) {
        viewModelScope.launch {
            chatSessionRepository.deleteSession(sessionId)
        }
    }
}