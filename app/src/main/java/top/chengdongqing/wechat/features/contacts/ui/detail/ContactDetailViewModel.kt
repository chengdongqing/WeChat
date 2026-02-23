package top.chengdongqing.wechat.features.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.model.ContactRelation
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.data.mapper.toContact
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@HiltViewModel(assistedFactory = ContactDetailViewModel.Factory::class)
class ContactDetailViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val contactRepository: ContactRepository,
    private val contactP2PRepository: ContactP2PRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): ContactDetailViewModel
    }

    val uiState: StateFlow<ContactDetailUiState> = combine(
        profileRepository.getCurrentProfile(),
        contactRepository.observeContactById(contactId)
    ) { myProfile, contact ->
        if (myProfile == null) {
            return@combine ContactDetailUiState(isLoading = false, error = "未找到个人资料")
        }

        val isMyself = contactId == myProfile.id

        val finalContact = when {
            isMyself -> {
                // 自己
                myProfile.toContact()
            }

            contact != null -> {
                // 朋友（从数据库）
                contact.copy(relation = ContactRelation.Friend)
            }

            else -> {
                // 陌生人（从缓存）
                contactP2PRepository.getContactFromCache(contactId)
            }
        }

        ContactDetailUiState(
            contact = finalContact,
            isLoading = false,
            error = if (finalContact == null) "未找到联系人信息" else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactDetailUiState(isLoading = true)
    )

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    /**
     * 处理联系人操作
     */
    fun handleAction(action: ContactAction) {
        viewModelScope.launch {
            when (action) {
                ContactAction.SendMessage -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToChat)
                }

                is ContactAction.VoiceVideoCall -> {
                    _navigationEvent.emit(NavigationEvent.LaunchCall(action.type))
                }

                ContactAction.ViewMoments -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToMoments)
                }

                ContactAction.ViewProfile -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToProfile)
                }

                ContactAction.ShowMore -> {
                    _navigationEvent.emit(NavigationEvent.ShowMoreOptions)
                }

                ContactAction.AddToContacts -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToRequestAdd)
                }
            }
        }
    }

    /**
     * 拉黑/取消拉黑联系人
     */
    fun toggleBlock() {
        viewModelScope.launch {
            uiState.value.contact?.let { contact ->
                contactRepository.updateContact(
                    contact.copy(isBlocked = !contact.isBlocked)
                )
            }
        }
    }

    /**
     * 删除联系人
     */
    fun deleteContact() {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contactId)
                _navigationEvent.emit(NavigationEvent.ContactDeleted)
            } catch (_: Exception) {
            }
        }
    }
}

data class ContactDetailUiState(
    val contact: Contact? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class NavigationEvent {
    data object NavigateToChat : NavigationEvent()
    data class LaunchCall(val type: CallType) : NavigationEvent()
    data object NavigateToMoments : NavigationEvent()
    data object NavigateToProfile : NavigationEvent()
    data object NavigateToRequestAdd : NavigationEvent()
    data object ShowMoreOptions : NavigationEvent()
    data object ContactDeleted : NavigationEvent()
}

sealed class ContactAction {
    data object SendMessage : ContactAction()
    data class VoiceVideoCall(val type: CallType) : ContactAction()
    data object ViewMoments : ContactAction()
    data object ViewProfile : ContactAction()
    data object ShowMore : ContactAction()
    data object AddToContacts : ContactAction()
}