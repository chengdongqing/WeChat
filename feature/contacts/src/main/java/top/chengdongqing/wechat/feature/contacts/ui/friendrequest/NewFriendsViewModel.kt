package top.chengdongqing.wechat.feature.contacts.ui.friendrequest

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.core.model.FriendRequest
import top.chengdongqing.wechat.core.network.model.NotificationId
import javax.inject.Inject

data class NewFriendsUiState(
    val requests: List<FriendRequest> = emptyList(),
    val filteredRequests: List<FriendRequest> = emptyList(), // 过滤后的展示数据
    val searchQuery: String = "", // 搜索框内容
    val pendingCount: Int = 0
)

@HiltViewModel
class NewFriendsViewModel @Inject constructor(
    private val friendRequestRepository: FriendRequestRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewFriendsUiState> = combine(
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

        NewFriendsUiState(
            requests = requests,
            filteredRequests = filtered,
            searchQuery = query,
            pendingCount = count
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewFriendsUiState()
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
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NotificationId.FriendRequest.id)
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