package top.chengdongqing.wechat.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository

@HiltViewModel
class HomeViewModel @Inject constructor(
    chatSessionRepository: ChatSessionRepository,
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(HomeTab.Chats)

    private val _unreadMapFlow: Flow<Map<HomeTab, Int>> = combine(
        chatSessionRepository.observeTotalUnreadCount(),
        friendRequestRepository.observeUnreadCount()
    ) { chatUnread, contactUnread ->
        mapOf(
            HomeTab.Chats to chatUnread,
            HomeTab.Contacts to contactUnread,
            HomeTab.Discover to 0,
            HomeTab.Me to 0
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _currentTab,
        _unreadMapFlow
    ) { currentTab, unreadMap ->
        HomeUiState(currentTab, unreadMap)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onTabSelected(tab: HomeTab) {
        _currentTab.update {
            tab
        }
    }
}