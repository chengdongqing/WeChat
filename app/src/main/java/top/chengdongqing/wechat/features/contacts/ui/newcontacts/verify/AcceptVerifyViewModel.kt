package top.chengdongqing.wechat.features.contacts.ui.newcontacts.verify

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
import top.chengdongqing.wechat.data.database.dao.FriendRequestDao
import top.chengdongqing.wechat.data.database.entity.toDomain
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import top.chengdongqing.wechat.features.contacts.domain.repository.FriendRequestRepository

@HiltViewModel(assistedFactory = AcceptVerifyViewModel.Factory::class)
class AcceptVerifyViewModel @AssistedInject constructor(
    @Assisted private val requestId: String,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendRequestDao: FriendRequestDao
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestId: String): AcceptVerifyViewModel
    }

    private val _uiState = MutableStateFlow(AcceptVerifyUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AcceptVerifyEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadRequest()
    }

    /**
     * 加载申请详情
     */
    private fun loadRequest() {
        viewModelScope.launch {
            try {
                val request = friendRequestDao.getById(requestId)

                if (request != null) {
                    _uiState.update {
                        it.copy(
                            request = request.toDomain(),
                            remark = request.greetingMessage.removePrefix("我是").trim(),
                            note = request.note ?: ""
                        )
                    }
                } else {
                    _eventFlow.emit(AcceptVerifyEvent.ShowError("申请不存在"))
                }
            } catch (e: Exception) {
                _eventFlow.emit(AcceptVerifyEvent.ShowError("加载失败: ${e.message}"))
            }
        }
    }

    fun updateRemark(text: String) {
        _uiState.update { it.copy(remark = text) }
    }

    fun updateNote(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    fun accept() {
        viewModelScope.launch {
            val state = _uiState.value

            _uiState.update { it.copy(isLoading = true) }

            friendRequestRepository.acceptFriendRequest(
                requestId = requestId,
                remark = state.remark.takeIf { it.isNotBlank() },
                tags = state.tags.takeIf { it.isNotEmpty() },
                note = state.note.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(AcceptVerifyEvent.AcceptSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(AcceptVerifyEvent.ShowError(e.message ?: "操作失败"))
                }
            )
        }
    }
}

data class AcceptVerifyUiState(
    val request: FriendRequest? = null,
    val remark: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class AcceptVerifyEvent {
    object AcceptSuccess : AcceptVerifyEvent()
    data class ShowError(val message: String) : AcceptVerifyEvent()
}