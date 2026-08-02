package top.chengdongqing.wechat.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository

@HiltViewModel
class MainShellViewModel @Inject constructor(
    chatSessionRepository: ChatSessionRepository,
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    // 各个 Tab 的未读消息数
    val unreadMap: StateFlow<Map<MainTab, Int>> = combine(
        chatSessionRepository.observeTotalUnreadCount(),
        friendRequestRepository.observeUnreadCount()
    ) { chatUnread, contactUnread ->
        mapOf(
            MainTab.Chats to chatUnread,
            MainTab.Contacts to contactUnread,
            MainTab.Discovery to 0,
            MainTab.Me to 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
}