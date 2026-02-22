package top.chengdongqing.wechat.features.home.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.data.network.service.NetworkService
import top.chengdongqing.wechat.data.network.service.createNetworkServiceIntent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.home.navigation.HomeTab

@HiltViewModel
class HomeViewModel @Inject constructor(
    chatSessionRepository: ChatSessionRepository,
    friendRequestRepository: FriendRequestRepository,
    @ApplicationContext context: Context
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

    /**
     * 启动后台网络服务
     */
    init {
        val intent = context.createNetworkServiceIntent(NetworkService.ACTION_START_CONNECT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}