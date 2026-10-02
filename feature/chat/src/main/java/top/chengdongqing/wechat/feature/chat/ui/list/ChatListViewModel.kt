package top.chengdongqing.wechat.feature.chat.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ConnectionSettingsRepository
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.network.session.ActiveSessionManager
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository,
    private val activeSessionManager: ActiveSessionManager,
    connectionSettingsRepository: ConnectionSettingsRepository
) : ViewModel() {

    private val _chats: Flow<List<ChatSession>> = chatSessionRepository.observeAllSessions()
    private val _connectionMode: Flow<ConnectionMode> = connectionSettingsRepository.connectionMode

    val uiState: StateFlow<ChatListUiState> = combine(
        _chats,
        _connectionMode
    ) { chats, connectionMode ->
        ChatListUiState(chats, connectionMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatListUiState()
    )

    fun onIntent(intent: ChatListUiIntent) {
        when (intent) {
            is ChatListUiIntent.MarkAsRead -> toggleReadMark(intent.chatId, true)
            is ChatListUiIntent.MarkAsUnread -> toggleReadMark(intent.chatId, false)
            is ChatListUiIntent.PinToTop -> togglePin(intent.chatId, true)
            is ChatListUiIntent.RemoveFromTop -> togglePin(intent.chatId, false)
            is ChatListUiIntent.HideChat -> hideChat(intent.chatId)
            is ChatListUiIntent.DeleteChat -> deleteChat(intent.chatId)
            is ChatListUiIntent.MarkEnterScreen -> activeSessionManager.enterList()
            is ChatListUiIntent.MarkLeaveScreen -> activeSessionManager.leaveList()
        }
    }

    /**
     * 标为已读/未读
     */
    private fun toggleReadMark(chatId: String, isRead: Boolean) {
        viewModelScope.launch {
            if (isRead) {
                chatSessionRepository.markAsRead(chatId)
            } else {
                chatSessionRepository.markAsUnread(chatId)
            }
        }
    }

    /**
     * 置顶/取消置顶聊天
     */
    private fun togglePin(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatSessionRepository.togglePin(chatId, !isPinned)
        }
    }

    /**
     * 隐藏聊天
     */
    private fun hideChat(chatId: String) {
        viewModelScope.launch {
            chatSessionRepository.hideSession(chatId)
        }
    }

    /**
     * 删除聊天
     */
    private fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatSessionRepository.deleteSession(chatId)
        }
    }
}