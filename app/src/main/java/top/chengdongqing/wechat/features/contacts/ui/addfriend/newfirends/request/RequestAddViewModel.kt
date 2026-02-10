package top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends.request

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
import top.chengdongqing.wechat.features.contacts.data.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.data.repository.FriendRequestRepository
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

data class RequestAddUiState(
    val contact: Contact? = null,
    val greetingMessage: String = "我是",
    val remark: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel(assistedFactory = RequestAddViewModel.Factory::class)
class RequestAddViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): RequestAddViewModel
    }

    private val _uiState = MutableStateFlow(RequestAddUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RequestAddEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
        val contact = ContactP2PRepository.getContactFromCache(contactId)
        _uiState.update { it.copy(contact = contact) }
    }

    fun updateGreeting(text: String) {
        _uiState.update { it.copy(greetingMessage = text) }
    }

    fun updateRemark(text: String) {
        _uiState.update { it.copy(remark = text) }
    }

    fun updateTags(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    fun updateNote(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    fun sendRequest() {
        viewModelScope.launch {
            val state = _uiState.value
            val contact = state.contact ?: return@launch

            if (state.greetingMessage.isBlank()) {
                _eventFlow.emit(RequestAddEvent.ShowError("请输入打招呼内容"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            friendRequestRepository.sendFriendRequest(
                targetContact = contact,
                greetingMessage = state.greetingMessage,
                remark = state.remark.takeIf { it.isNotBlank() },
                tags = state.tags.takeIf { it.isNotEmpty() },
                note = state.note.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(RequestAddEvent.SendSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(RequestAddEvent.ShowError(e.message ?: "发送失败"))
                }
            )
        }
    }
}

sealed class RequestAddEvent {
    object SendSuccess : RequestAddEvent()
    data class ShowError(val message: String) : RequestAddEvent()
}