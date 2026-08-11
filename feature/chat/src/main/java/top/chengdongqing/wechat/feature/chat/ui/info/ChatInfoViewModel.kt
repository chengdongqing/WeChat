package top.chengdongqing.wechat.feature.chat.ui.info

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.model.ChatSession
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.toContact
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.feature.chat.ai.LocalAiEngine
import top.chengdongqing.wechat.feature.chat.ai.LocalAiModelInfo
import top.chengdongqing.wechat.feature.chat.ai.LocalAiState

data class ChatInfoUiState(
    /** 联系人信息 */
    val contactName: String = "",
    val contactAvatar: String? = null,

    /** 会话设置 */
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBottomed: Boolean = false,
    val backgroundPath: String? = null,
    val isTemporary: Boolean = false,
    val expiresAt: Long? = null,
    val isFriend: Boolean = false,
    val isLocalAi: Boolean = false,
    val localAiState: LocalAiState = LocalAiState.NoModel,
    val modelSizeBytes: Long? = null,
    val modelInfo: LocalAiModelInfo? = null
)

@HiltViewModel(assistedFactory = ChatInfoViewModel.Factory::class)
class ChatInfoViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val chatSessionRepository: ChatSessionRepository,
    private val contactRepository: ContactRepository,
    private val profileRepository: ProfileRepository,
    private val privateFileManager: PrivateFileManager,
    private val localAiEngine: LocalAiEngine,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private var modelImportJob: Job? = null

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatInfoViewModel
    }

    companion object {
        private const val TAG = "ChatInfoVM"
    }

    init {
        ensureSessionExists()
        promoteWhenFriendAdded()
    }

    private fun promoteWhenFriendAdded() {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.observeContact(chatId).collect { contact ->
                if (contact != null && chatSessionRepository.getSession(chatId)?.isTemporary == true) {
                    chatSessionRepository.setTemporary(chatId, null)
                }
            }
        }
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
        contactRepository.observeContact(chatId),
        localAiEngine.state
    ) { myProfile, session, contact, localAiState ->
        if (myProfile == null || session == null) {
            return@combine ChatInfoUiState()
        }

        val isLocalAi = chatId == LocalAiAssistant.ID
        val isSelf = chatId == myProfile.id
        val finalContact = if (isSelf) {
            myProfile.toContact()
        } else {
            contact
        }

        ChatInfoUiState(
            contactName = if (isLocalAi) LocalAiAssistant.NAME
            else finalContact?.displayName ?: session.contactName,
            contactAvatar = finalContact?.avatarPath ?: session.contactAvatar,
            isMuted = session.isMuted,
            isPinned = session.isPinned,
            isBottomed = session.isBottomed,
            backgroundPath = session.backgroundPath,
            isTemporary = session.isTemporary,
            expiresAt = session.expiresAt,
            isFriend = contact != null,
            isLocalAi = isLocalAi,
            localAiState = localAiState,
            modelSizeBytes = localAiEngine.modelSizeBytes,
            modelInfo = localAiEngine.modelInfo
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

    fun toggleBottomed() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.toggleBottom(chatId, !uiState.value.isBottomed)
        }
    }

    fun endTemporaryChat(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(chatId, shouldHide = true)
            withContext(Dispatchers.Main) {
                context.showToast("临时聊天已结束")
                onComplete()
            }
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

    fun importLocalAiModel(uri: Uri) {
        modelImportJob?.cancel()
        modelImportJob = viewModelScope.launch {
            runCatching { localAiEngine.importModel(uri) }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        context.showToast(error.message ?: "模型导入失败")
                    }
                }
        }
    }

    fun cancelModelLoading() {
        modelImportJob?.cancel()
        viewModelScope.launch {
            localAiEngine.cancelLoading()
            context.showToast("已取消模型加载")
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            localAiEngine.unloadModel()
            context.showToast("模型已卸载")
        }
    }
}
