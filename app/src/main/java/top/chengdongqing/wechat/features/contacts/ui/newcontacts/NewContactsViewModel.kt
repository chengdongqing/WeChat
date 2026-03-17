package top.chengdongqing.wechat.features.contacts.ui.newcontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.model.NotificationId
import top.chengdongqing.wechat.data.network.service.notification.NotificationHelper
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository
import javax.inject.Inject

data class NewContactsUiState(
    val requests: List<FriendRequest> = emptyList(),
    val filteredRequests: List<FriendRequest> = emptyList(), // 过滤后的展示数据
    val searchQuery: String = "", // 搜索框内容
    val pendingCount: Int = 0
)

@HiltViewModel
class NewContactsViewModel @Inject constructor(
    private val friendRequestRepository: FriendRequestRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewContactsUiState> = combine(
        friendRequestRepository.observeAllRequests(),
        friendRequestRepository.getPendingCount(),
        _searchQuery
    ) { requests, count, query ->
        // 匹配昵称或微信号
        val filtered = if (query.isBlank()) {
            requests
        } else {
            requests.filter {
                it.nickname.contains(query, ignoreCase = true) ||
                        it.userId.contains(query, ignoreCase = true)
            }
        }

        NewContactsUiState(
            requests = requests,
            filteredRequests = filtered,
            searchQuery = query,
            pendingCount = count
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewContactsUiState()
    )

    init {
        clearUnreadState()
        checkAndMarkExpired()
    }

    /**
     * 检查及标记过期
     */
    private fun checkAndMarkExpired() {
        viewModelScope.launch {
            friendRequestRepository.checkAndMarkExpired()
        }
    }

    private fun clearUnreadState() {
        // 标记已读
        viewModelScope.launch {
            friendRequestRepository.markAllIncomingAsRead()
        }

        // 清除通知
        notificationHelper.cancelNotification(NotificationId.FriendRequest.id)
    }

    fun updateQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun delete(requestId: String) {
        viewModelScope.launch {
            friendRequestRepository.deleteRequest(requestId)
        }
    }
}