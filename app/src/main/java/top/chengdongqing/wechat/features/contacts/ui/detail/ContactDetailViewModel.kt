package top.chengdongqing.wechat.features.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.call.model.CallType
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

    val contact: StateFlow<Contact?> = combine(
        profileRepository.observeProfile(),
        contactRepository.observeContact(contactId)
    ) { myProfile, contact ->
        if (myProfile == null) {
            return@combine null
        }

        val isSelf = contactId == myProfile.id

        val finalContact = when {
            isSelf -> {
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

        finalContact
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

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
            contactRepository.updateContact(
                contact.value?.id ?: return@launch
            ) { contact ->
                contact.copy(isBlocked = !contact.isBlocked)
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
                _uiState.update { it.copy(error = "删除联系人失败") }
            }
        }
    }
}

data class ContactDetailUiState(
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