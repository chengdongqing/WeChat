package top.chengdongqing.wechat.features.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@HiltViewModel(assistedFactory = ContactDetailViewModel.Factory::class)
class ContactDetailViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val contactRepository: ContactRepository,
    private val contactP2PRepository: ContactP2PRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): ContactDetailViewModel
    }

    private val _uiState = MutableStateFlow(ContactDetailUiState(isLoading = true))
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadContactDetails()
    }

    /**
     * 加载联系人详细信息
     */
    private fun loadContactDetails() {
        viewModelScope.launch {
            try {
                // 获取当前用户ID
                val myProfile = profileRepository.getCurrentProfileOnce() ?: return@launch
                val myUserId = myProfile.id

                // 检查是否是自己
                val isMyself = contactId == myUserId

                // 先尝试从数据库加载
                var contact = if (!isMyself) contactRepository.getContactById(contactId) else null
                val isFriend = contact != null

                contact = when {
                    isMyself -> {
                        // 如果是自己，从 Profile 动态创建
                        Contact(
                            id = myProfile.id,
                            nickname = myProfile.nickname,
                            avatarPath = myProfile.avatarPath,
                            signature = myProfile.signature,
                            gender = myProfile.gender,
                            relation = ContactRelation.Myself
                        )
                    }

                    !isFriend -> {
                        // 尝试从缓存获取（扫码进入）
                        contactP2PRepository.getContactFromCache(contactId)
                    }

                    else -> {
                        // 朋友
                        contact.copy(relation = ContactRelation.Friend)
                    }
                }

                if (contact != null) {
                    _uiState.update {
                        it.copy(
                            contact = contact,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "未找到联系人信息"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载联系人信息失败：${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 处理联系人操作
     */
    fun handleAction(action: ContactAction) {
        viewModelScope.launch {
            when (action) {
                ContactAction.SendMessage -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToChat(contactId))
                }

                ContactAction.VoiceVideoCall -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToCall(contactId))
                }

                ContactAction.ViewMoments -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToMoments(contactId))
                }

                ContactAction.ViewProfile -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToProfile(contactId))
                }

                ContactAction.ShowMore -> {
                    _navigationEvent.emit(NavigationEvent.ShowMoreOptions(contactId))
                }

                ContactAction.DeleteContact -> {
                    deleteContact()
                }

                ContactAction.AddToContacts -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToRequestAdd(contactId))
                }
            }
        }
    }

    /**
     * 删除联系人
     */
    private fun deleteContact() {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contactId)
                _navigationEvent.emit(NavigationEvent.ContactDeleted)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除失败：${e.message}") }
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ContactDetailUiState(
    val contact: Contact? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class NavigationEvent {
    data class NavigateToChat(val contactId: String) : NavigationEvent()
    data class NavigateToCall(val contactId: String) : NavigationEvent()
    data class NavigateToMoments(val contactId: String) : NavigationEvent()
    data class NavigateToProfile(val contactId: String) : NavigationEvent()
    data class NavigateToRequestAdd(val contactId: String) : NavigationEvent()
    data class ShowMoreOptions(val contactId: String) : NavigationEvent()
    data object ContactDeleted : NavigationEvent()
}

sealed class ContactAction {
    data object SendMessage : ContactAction()
    data object VoiceVideoCall : ContactAction()
    data object ViewMoments : ContactAction()
    data object ViewProfile : ContactAction()
    data object ShowMore : ContactAction()
    data object DeleteContact : ContactAction()
    data object AddToContacts : ContactAction()
}