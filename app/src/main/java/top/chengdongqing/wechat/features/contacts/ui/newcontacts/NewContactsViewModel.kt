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
    private val friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NewContactsUiState> = combine(
        friendRequestRepository.getRequests(),
        friendRequestRepository.getPendingCount(),
        _searchQuery
    ) { requests, count, query ->
        // 匹配昵称或微信号
        val filtered = if (query.isBlank()) {
            requests
        } else {
            requests.filter {
                it.peerNickname.contains(query, ignoreCase = true) ||
                        it.peerUserId.contains(query, ignoreCase = true)
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

    fun markAllAsRead() {
        viewModelScope.launch {
            friendRequestRepository.markAllIncomingAsRead()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun delete(requestId: String) {
        viewModelScope.launch {
            friendRequestRepository.delete(requestId)
        }
    }
}