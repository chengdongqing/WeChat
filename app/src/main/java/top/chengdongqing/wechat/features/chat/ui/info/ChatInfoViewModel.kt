package top.chengdongqing.wechat.features.chat.ui.info

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.features.chat.domain.model.ChatSession
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.profile.data.mapper.toContact
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository

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
    private val privateFileManager: PrivateFileManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatInfoViewModel
    }

    companion object {
        private const val TAG = "ChatInfoVM"
    }

    init {
        ensureSessionExists()
    }

    private fun ensureSessionExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val existSession = chatSessionRepository.exists(chatId)
            if (!existSession) {
                val myProfile = profileRepository.requireProfile()
                val contact = contactRepository.getContact(chatId)
                val isSelf = chatId == myProfile.id

                chatSessionRepository.createSession(
                    ChatSession(
                        id = chatId,
                        contactId = chatId,
                        contactName = if (isSelf) myProfile.nickname else contact?.displayName
                            ?: "",
                        contactAvatar = if (isSelf) myProfile.avatarPath else contact?.avatarPath,
                        isHidden = true, // 初始隐藏，发消息才显示
                    )
                )
            }
        }
    }

    val uiState = combine(
        profileRepository.observeProfile(),
        chatSessionRepository.observeSession(chatId),
        contactRepository.observeContact(chatId)
    ) { myProfile, session, contact ->
        if (myProfile == null || session == null) {
            return@combine ChatInfoUiState()
        }

        val isSelf = chatId == myProfile.id
        val finalContact = if (isSelf) {
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
            try {
                val oldPath = uiState.value.backgroundPath
                // 保存新背景
                val newPath = uri?.let { uri ->
                    privateFileManager.saveMedia(
                        messageType = MessageType.Image,
                        sourceUri = uri
                    ).getOrThrow()
                }
                // 更新数据
                chatSessionRepository.updateBackground(chatId, newPath)
                // 删除旧背景
                oldPath?.let { privateFileManager.deleteFile(it) }

                context.showToast(if (uri == null) "背景清除成功" else "背景设置成功")
            } catch (e: Exception) {
                Log.e(TAG, "更新背景图片失败", e)
                context.showToast("背景设置失败")
            }
        }
    }

    fun clearMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(chatId, false)
        }
    }
}