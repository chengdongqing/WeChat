package top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import javax.inject.Inject

data class NewFriendsUiState(
    val requests: List<FriendRequest> = emptyList(),
    val pendingCount: Int = 0
)

@HiltViewModel
class NewFriendsViewModel @Inject constructor(
    friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    val uiState: StateFlow<NewFriendsUiState> = combine(
        friendRequestRepository.getIncomingRequests(),
        friendRequestRepository.getPendingCount()
    ) { requests, count ->
        NewFriendsUiState(
            requests = requests,
            pendingCount = count
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewFriendsUiState()
    )
}