package top.chengdongqing.wechat.feature.contacts.ui.detail.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.handler.FileHandler
import top.chengdongqing.wechat.core.data.repository.AddFriendRepository
import top.chengdongqing.wechat.core.data.repository.MessageRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.toContact
import top.chengdongqing.wechat.core.model.toResult
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.contacts.domain.repository.ContactRepository

@HiltViewModel(assistedFactory = ContactSettingViewModel.Factory::class)
class ContactSettingViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val contactRepository: ContactRepository,
    profileRepository: ProfileRepository,
    private val addFriendRepository: AddFriendRepository,
    private val messageRepository: MessageRepository,
    private val privateFileManager: PrivateFileManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): ContactSettingViewModel
    }

    val contact = combine(
        profileRepository.observeProfile(),
        contactRepository.observeContact(contactId)
    ) { myProfile, contact ->
        if (myProfile == null) {
            return@combine null
        }

        val isSelf = contactId == myProfile.id
        val isAi = contactId == LocalAiAssistant.ID

        when {
            // 自己
            isSelf -> myProfile.toContact()
            // AI 助手
            isAi -> LocalAiAssistant.toContact(
                name = context.getString(R.string.local_ai_assistant_name),
                signature = context.getString(R.string.local_ai_assistant_signature)
            )
            // 朋友（从数据库）
            contact != null -> contact
            // 陌生人（从缓存）
            else -> addFriendRepository.getContactFromCache(contactId)
        }
    }

    val uiState = combine(contact) { (contact) ->
        ContactSettingUiState(contact)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactSettingUiState()
    )

    private val _uiEvent = Channel<ContactSettingUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onIntent(intent: ContactSettingUiIntent) {
        when (intent) {
            is ContactSettingUiIntent.ToggleStar -> toggleStar()
            is ContactSettingUiIntent.ToggleBlock -> toggleBlock()
            is ContactSettingUiIntent.DeleteContact -> deleteContact()
            is ContactSettingUiIntent.ShareContact -> shareContact(intent.targetContactId)
            else -> Unit
        }
    }

    /**
     * 开/关星标
     */
    fun toggleStar() {
        viewModelScope.launch {
            contactRepository.updateContact(contactId) { contact ->
                contact.copy(isStarred = !contact.isStarred)
            }
        }
    }

    /**
     * 拉黑/取消拉黑联系人
     */
    fun toggleBlock() {
        viewModelScope.launch {
            contactRepository.updateContact(contactId) { contact ->
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
                _uiEvent.send(ContactSettingUiEvent.OnContactDeleted)
            } catch (_: Exception) {
                context.showToast("删除联系人失败")
            }
        }
    }

    /**
     * 发送联系人名片消息
     */
    fun shareContact(targetContactId: String) {
        val handler = FileHandler(privateFileManager) {
            viewModelScope.launch {
                messageRepository.sendMessage(
                    sessionId = targetContactId,
                    receiverId = targetContactId,
                    content = it
                ).onSuccess {
                    context.showToast("已发送")
                }
            }
        }

        uiState.value.contact?.let {
            viewModelScope.launch {
                handler.handleContactSelection(it.toResult())
            }
        }
    }
}