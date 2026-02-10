package top.chengdongqing.wechat.features.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.home.navigation.HomeTab

@HiltViewModel
class HomeViewModel @Inject constructor(
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    // 各个 Tab 的未读消息数
    val unreadCounts: StateFlow<Map<HomeTab, Int>> = combine(
        flowOf(0),
        friendRequestRepository.getPendingCount()
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