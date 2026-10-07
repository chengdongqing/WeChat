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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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
import top.chengdongqing.wechat.feature.chat.R
import top.chengdongqing.wechat.feature.chat.ai.LocalAiEngine
import top.chengdongqing.wechat.feature.chat.ai.LocalAiError
import top.chengdongqing.wechat.feature.chat.ai.getLocalAiErrorMessage
import top.chengdongqing.wechat.core.designsystem.R as DesignR

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

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatInfoViewModel
    }

    companion object {
        private const val TAG = "ChatInfoVM"
    }

    private var modelImportJob: Job? = null

    private val _uiEvent = Channel<ChatInfoUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        ensureSessionExists()
        promoteWhenFriendAdded()
    }

    /**
     * 添加为好友后去掉临时会话标识
     */
    private fun promoteWhenFriendAdded() {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.observeContact(chatId).collect { contact ->
                if (contact != null && chatSessionRepository.getSession(chatId)?.isTemporary == true) {
                    chatSessionRepository.setTemporary(chatId, null)
                }
            }
        }
    }

    /**
     * 在没有创建会话记录时自动创建会话
     */
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

        val isSelf = chatId == myProfile.id
        val isAi = chatId == LocalAiAssistant.ID
        val finalContact = when {
            isSelf -> myProfile.toContact()
            isAi -> LocalAiAssistant.toContact(
                name = context.getString(DesignR.string.local_ai_assistant_name),
                signature = context.getString(DesignR.string.local_ai_assistant_signature)
            )

            else -> contact
        }

        ChatInfoUiState(
            contactName = finalContact?.displayName ?: session.contactName,
            contactAvatar = finalContact?.avatarPath ?: session.contactAvatar,
            isMuted = session.isMuted,
            isPinned = session.isPinned,
            isBottomed = session.isBottomed,
            backgroundPath = session.backgroundPath,
            isTemporary = session.isTemporary,
            expiresAt = session.expiresAt,
            isFriend = contact != null,
            isAiAssistant = isAi,
            localAiState = localAiState,
            modelSizeBytes = localAiEngine.modelSizeBytes,
            modelInfo = localAiEngine.modelInfo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatInfoUiState()
    )

    fun onIntent(intent: ChatInfoUiIntent) {
        when (intent) {
            is ChatInfoUiIntent.ToggleMuted -> toggleMuted()
            is ChatInfoUiIntent.TogglePinned -> togglePinned()
            is ChatInfoUiIntent.ToggleBottomed -> toggleBottomed()
            is ChatInfoUiIntent.ClearChatHistory -> clearChatHistory()
            is ChatInfoUiIntent.EndTemporaryChat -> endTemporaryChat()
            is ChatInfoUiIntent.UpdateBackground -> updateBackground(intent.uri)
            is ChatInfoUiIntent.ImportLocalAiModel -> importLocalAiModel(intent.uri)
            is ChatInfoUiIntent.CancelModelLoading -> cancelModelLoading()
            is ChatInfoUiIntent.UnloadModel -> unloadModel()
        }
    }

    private fun toggleMuted() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.toggleMute(chatId, !uiState.value.isMuted)
        }
    }

    private fun togglePinned() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.togglePin(chatId, !uiState.value.isPinned)
        }
    }

    private fun toggleBottomed() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.toggleBottom(chatId, !uiState.value.isBottomed)
        }
    }

    private fun endTemporaryChat() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(chatId, shouldHide = true)
            withContext(Dispatchers.Main) {
                context.showToast("临时聊天已结束")
                _uiEvent.send(ChatInfoUiEvent.OnTemporaryChatEnd)
            }
        }
    }

    private fun updateBackground(uri: Uri?) {
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

    private fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            chatSessionRepository.deleteSession(chatId, false)
        }
    }

    private fun importLocalAiModel(uri: Uri) {
        modelImportJob?.cancel()
        modelImportJob = viewModelScope.launch {
            runCatching { localAiEngine.importModel(uri) }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        context.showToast(
                            context.getLocalAiErrorMessage(error, LocalAiError.MODEL_IMPORT_FAILED)
                        )
                    }
                }
        }
    }

    private fun cancelModelLoading() {
        modelImportJob?.cancel()
        viewModelScope.launch {
            localAiEngine.cancelLoading()
            context.showToast(context.getString(R.string.local_ai_error_loading_cancelled))
        }
    }

    private fun unloadModel() {
        viewModelScope.launch {
            localAiEngine.unloadModel()
            context.showToast("模型已卸载")
        }
    }
}
