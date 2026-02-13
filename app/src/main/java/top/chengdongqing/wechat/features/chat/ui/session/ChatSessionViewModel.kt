package top.chengdongqing.wechat.features.chat.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.util.AudioPlaybackManager
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val activeSessionManager: ActiveSessionManager,
    soundTipPlayer: SoundTipPlayer,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    // ==================== UI 状态 ====================

    private val _uiState = MutableStateFlow(ChatSessionUiState())
    val uiState = _uiState.asStateFlow()

    // 消息流
    val messages = messageRepository
        .observeMessages(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 派生状态：媒体预览列表
    val mediaList = messages
        .map { list ->
            list.asSequence()
                .mapNotNull { it.content as? MessageContent.Media }
                .toList()
                .reversed()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ==================== 播放管理 ====================

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { _playingMessageId.value = it },
        onMessagePlayed = { markAsPlayed(it) }
    )

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    private var lastLoadedTimestamp: Long? = null

    init {
        activeSessionManager.enter(chatId)
        loadInitialData()
    }

    // ==================== 初始化逻辑 ====================

    private fun loadInitialData() {
        viewModelScope.launch {
            val contactDeferred = async(Dispatchers.IO) {
                contactRepository.getContactById(chatId)
            }
            val profileDeferred = async(Dispatchers.IO) {
                profileRepository.getCurrentProfileOnce()
            }

            try {
                val contact = contactDeferred.await()
                val profile = profileDeferred.await()

                // 更新基础 UI 状态
                _uiState.update {
                    it.copy(
                        title = contact?.displayName ?: profile?.nickname ?: "",
                        peerAvatar = contact?.avatarPath,
                        myAvatar = profile?.avatarPath
                    )
                }

                // 加载第一页消息
                loadInitialMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 标记已读
            launch(Dispatchers.IO) {
                messageRepository.markAllAsRead(chatId)
            }
        }
    }

    private suspend fun loadInitialMessages() {
        val result = withContext(Dispatchers.IO) {
            runCatching { messageRepository.getMessages(chatId, PAGE_SIZE) }
        }

        result.onSuccess { msgs ->
            lastLoadedTimestamp = msgs.lastOrNull()?.timestamp
            _uiState.update {
                it.copy(
                    hasMoreMessages = msgs.size >= PAGE_SIZE,
                    shouldScrollToBottom = true
                )
            }
        }.onFailure {
            _uiState.update { it.copy(hasMoreMessages = false) }
        }
    }

    // ==================== 交互操作 ====================

    fun sendMessage(content: MessageContent, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            messageRepository.sendMessage(chatId, chatId, content)
                .onSuccess {
                    onSent()
                }
        }
    }

    fun finishSending() {
        _uiState.update { it.copy(isSending = false) }
    }

    /**
     * 加载更多历史
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    messageRepository.getMessages(chatId, PAGE_SIZE, lastLoadedTimestamp)
                }
            }

            result.onSuccess { moreMsgs ->
                if (moreMsgs.isNotEmpty()) {
                    lastLoadedTimestamp = moreMsgs.lastOrNull()?.timestamp
                }
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        hasMoreMessages = moreMsgs.size >= PAGE_SIZE
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * 重试发送失败的消息
     */
    fun retrySend(messageId: String) {
        viewModelScope.launch {
            messageRepository.retrySend(messageId)
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun toggleVoicePlay(messageId: String, localPath: String) {
        val voiceMessages = messages.value.filter { it.content is MessageContent.Voice }
        audioPlaybackManager.togglePlay(messageId, localPath, voiceMessages)
    }

    fun stopVoice() {
        audioPlaybackManager.stop()
    }

    fun onScrolledToBottomHandled() {
        _uiState.update { it.copy(shouldScrollToBottom = false) }
    }

    private fun markAsPlayed(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // messageRepo.markVoiceAsPlayed(messageId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
        activeSessionManager.leave()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

data class ChatSessionUiState(
    val title: String = "",
    val peerAvatar: String? = null,
    val myAvatar: String? = null,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val shouldScrollToBottom: Boolean = false
)