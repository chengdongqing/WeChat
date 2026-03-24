package top.chengdongqing.wechat.feature.home.ui

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
import top.chengdongqing.wechat.feature.home.model.HomeTab

@HiltViewModel
class HomeViewModel @Inject constructor(
    chatSessionRepository: ChatSessionRepository,
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    // 各个 Tab 的未读消息数
    val unreadCounts: StateFlow<Map<HomeTab, Int>> = combine(
        chatSessionRepository.observeTotalUnreadCount(),
        friendRequestRepository.observeUnreadCount()
    ) { chatUnread, contactUnread ->
        mapOf(
            HomeTab.Chats to chatUnread,
            HomeTab.Contacts to contactUnread,
            HomeTab.Discovery to 0,
            HomeTab.Me to 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
}