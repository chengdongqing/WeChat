package top.chengdongqing.wechat.features.contacts.ui.detail.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository

data class EditContactProfileUiState(
    val contact: Contact? = null,
    val remarkName: String = "",
    val note: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed class EditProfileEvent {
    data object SaveSuccess : EditProfileEvent()
    data class SaveError(val message: String) : EditProfileEvent()
}

@HiltViewModel(assistedFactory = EditContactProfileViewModel.Factory::class)
class EditContactProfileViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val contactRepository: ContactRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): EditContactProfileViewModel
    }

    private val _uiState = MutableStateFlow(EditContactProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProfileEvent>()
    val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        loadContact()
    }

    /**
     * 加载联系人信息
     */
    private fun loadContact() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val contact = contactRepository.getContact(contactId)

                if (contact != null) {
                    _uiState.update {
                        it.copy(
                            contact = contact,
                            remarkName = contact.remarkName ?: "",
                            note = contact.note ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "未找到联系人"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败：${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 更新备注名
     */
    fun updateRemarkName(remarkName: String) {
        _uiState.update { it.copy(remarkName = remarkName) }
    }

    /**
     * 更新备忘
     */
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    /**
     * 保存修改
     */
    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val currentState = _uiState.value

                contactRepository.updateContact(
                    currentState.contact?.id ?: return@launch
                ) { contact ->
                    contact.copy(
                        remarkName = currentState.remarkName.ifBlank { null },
                        note = currentState.note.ifBlank { null }
                    )
                }

                _events.emit(EditProfileEvent.SaveSuccess)
            } catch (e: Exception) {
                _events.emit(EditProfileEvent.SaveError(e.message ?: "保存失败"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}