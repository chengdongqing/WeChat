package top.chengdongqing.wechat.features.chat.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.util.AudioPlaybackManager

data class ChatSessionState(
    val title: String = "",
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true
)

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val messageRepository: MessageRepository,
    private val chatSessionRepository: ChatSessionRepository,
    soundTipPlayer: SoundTipPlayer,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    // ==================== 状态 ====================

    private val _uiState = MutableStateFlow(ChatSessionState())
    val uiState: StateFlow<ChatSessionState> = _uiState.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = messageRepository
        .observeMessages(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 媒体列表（从消息派生）
    val mediaList: StateFlow<List<MessageContent.Media>> = messages
        .map { list ->
            list.mapNotNull { msg ->
                (msg.content as? MessageContent.Media)
            }.reversed()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 音频播放管理
    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { messageId -> _playingMessageId.value = messageId },
        onMessagePlayed = { messageId -> markAsPlayed(messageId) }
    )

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    // 分页相关
    private var oldestTimestamp: Long? = null

    init {
        loadInitialData()
    }

    // ==================== 初始化 ====================

    private fun loadInitialData() {
        viewModelScope.launch {
            // 加载会话信息
            val session = chatSessionRepository.getSession(chatId)
            _uiState.update { it.copy(title = session?.contactName ?: "") }

            // 标记已读
            messageRepository.markAllAsRead(chatId)

            // 加载初始消息
            loadInitialMessages()
        }
    }

    private suspend fun loadInitialMessages() {
        try {
            val initialMessages = messageRepository.getMessages(
                sessionId = chatId,
                limit = PAGE_SIZE
            )

            oldestTimestamp = initialMessages.lastOrNull()?.timestamp

            _uiState.update {
                it.copy(
                    hasMoreMessages = initialMessages.size >= PAGE_SIZE
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(hasMoreMessages = false) }
        }
    }

    // ==================== 消息操作 ====================

    /**
     * 发送消息
     */
    fun sendMessage(content: MessageContent, onSent: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,  // P2P 场景下 sessionId == receiverId
                content = content
            ).onSuccess {
                onSent()
            }.onFailure {
                // 发送失败会在消息列表中自动显示失败状态
            }
        }
    }

    fun finishSending() {
        _uiState.update { it.copy(isSending = false) }
    }

    /**
     * 加载更多历史消息
     */
    fun loadMore(lastVisibleMsgId: String) {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val moreMessages = messageRepository.getMessages(
                    sessionId = chatId,
                    limit = PAGE_SIZE,
                    beforeTimestamp = oldestTimestamp
                )

                if (moreMessages.isNotEmpty()) {
                    oldestTimestamp = moreMessages.lastOrNull()?.timestamp
                }

                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        hasMoreMessages = moreMessages.size >= PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
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

    // ==================== 语音播放 ====================

    fun toggleVoicePlay(messageId: String, localPath: String) {
        val voiceMessages = messages.value.filter {
            it.content is MessageContent.Voice
        }
        audioPlaybackManager.togglePlay(messageId, localPath, voiceMessages)
    }

    fun stopVoice() {
        audioPlaybackManager.stop()
    }

    private fun markAsPlayed(messageId: String) {
        viewModelScope.launch {
            // Room 监听会自动更新 UI
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
    }

    // ==================== 常量 ====================

    companion object {
        const val PAGE_SIZE = 20
    }
}