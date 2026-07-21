package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.AddFriendRepository
import top.chengdongqing.wechat.core.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.core.model.Contact

data class RequestAddFriendUiState(
    val contact: Contact? = null,
    val greeting: String = "",
    val remark: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel(assistedFactory = RequestAddFriendViewModel.Factory::class)
class RequestAddFriendViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val friendRequestRepository: FriendRequestRepository,
    private val addFriendRepository: AddFriendRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): RequestAddFriendViewModel
    }

    private val _uiState = MutableStateFlow(RequestAddFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SendEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
        val contact = addFriendRepository.getContactFromCache(contactId)
        _uiState.update { it.copy(contact = contact) }
    }

    fun updateGreeting(text: String) {
        _uiState.update { it.copy(greeting = text) }
    }

    fun updateRemark(text: String) {
        _uiState.update { it.copy(remark = text) }
    }

    fun updateNote(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    fun sendRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            val contact = state.contact ?: return@launch

            _uiState.update { it.copy(isLoading = true) }

            friendRequestRepository.sendFriendRequest(
                targetContact = contact,
                greeting = state.greeting,
                remark = state.remark.takeIf { it.isNotBlank() },
                note = state.note.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = {
                    _eventFlow.emit(SendEvent.WaitingVerify)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(SendEvent.Error(e.message ?: "发送失败"))
                }
            )
        }
    }
}

sealed class SendEvent {
    object WaitingVerify : SendEvent()
    data class Error(val message: String) : SendEvent()
}