package top.chengdongqing.wechat.features.chat.ui.info

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.manager.FileManager
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.data.mapper.toContact
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

data class ChatInfoUiState(
    /** 联系人信息 */
    val contactName: String = "",
    val contactAvatar: String? = null,

    /** 会话设置 */
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val backgroundPath: String? = null
)

@HiltViewModel(assistedFactory = ChatInfoViewModel.Factory::class)
class ChatInfoViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val fileManager: FileManager
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatInfoViewModel
    }

    init {
        ensureSessionExists()
    }

    private fun ensureSessionExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val existSession = chatSessionRepository.exists(chatId)
            if (!existSession) {
                val myProfile = profileRepository.getCurrentProfileSnapshot()
                val contact = contactRepository.getContactById(chatId)
                val isMyself = chatId == myProfile?.id

                chatSessionRepository.insertSession(
                    ChatSession(
                        sessionId = chatId,
                        contactId = chatId,
                        contactName = if (isMyself) myProfile.nickname else contact?.displayName
                            ?: "",
                        contactAvatar = if (isMyself) myProfile.avatarPath else contact?.avatarPath,
                        isHidden = true, // 初始隐藏，发消息才显示
                    )
                )
            }
        }
    }

    val uiState = combine(
        profileRepository.getCurrentProfile(),
        chatSessionRepository.observeSession(chatId),
        contactRepository.observeContactById(chatId)
    ) { myProfile, session, contact ->
        if (myProfile == null || session == null) {
            return@combine ChatInfoUiState()
        }

        val isMyself = chatId == myProfile.id
        val finalContact = if (isMyself) {
            myProfile.toContact()
        } else {
            contact
        }

        ChatInfoUiState(
            contactName = finalContact?.displayName ?: session.contactName,
            contactAvatar = finalContact?.avatarPath ?: session.contactAvatar,
            isMuted = session.isMuted,
            isPinned = session.isPinned,
            backgroundPath = session.backgroundPath
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatInfoUiState()
    )

    fun toggleMuted() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.toggleMute(chatId, !uiState.value.isMuted)
        }
    }

    fun togglePinned() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.togglePin(chatId, !uiState.value.isPinned)
        }
    }

    fun updateBackground(uri: Uri?) {
        viewModelScope.launch {
            // 保存新头像
            val backgroundPath = uri?.let { uri ->
                fileManager.saveImage(uri).getOrThrow()
            }
            // 删除旧头像
            uiState.value.backgroundPath?.let {
                fileManager.deleteMediaFile(it)
            }
            chatSessionRepository.updateBackground(chatId, backgroundPath)
        }
    }

    fun clearMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(chatId, false)
        }
    }
}