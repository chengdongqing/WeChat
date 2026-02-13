package top.chengdongqing.wechat.features.chat.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatSessionRepository: ChatSessionRepository
) : ViewModel() {

    val chats = chatSessionRepository
        .observeAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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