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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.session.ActiveSessionManager
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.ChatSessionRepository
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.chat.util.AudioPlaybackManager
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    chatSessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository,
    private val profileRepository: ProfileRepository,
    private val contactRepository: ContactRepository,
    private val contactP2PRepository: ContactP2PRepository,
    val activeSessionManager: ActiveSessionManager,
    e2eSessionManager: E2ESessionManager,
    soundTipPlayer: SoundTipPlayer,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(ChatSessionUiState())
    val uiState = _uiState.asStateFlow()

    private val _visibleCount = MutableStateFlow(PAGE_SIZE)

    // 消息流
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _visibleCount.flatMapLatest { count ->
        messageRepository.observeMessages(chatId, count)
    }.stateIn(
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // 是否启用了加密
    val isE2EActive = e2eSessionManager.encryptedPeers
        .map { it.contains(chatId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    // 未读数
    val unreadCount = chatSessionRepository.observeTotalUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    private val sessionFlow = chatSessionRepository.observeSession(chatId)

    private val audioPlaybackManager = AudioPlaybackManager(
        context = context,
        soundTipPlayer = soundTipPlayer,
        onPlayingStateChanged = { _playingMessageId.value = it },
        onMessagePlayed = { markAsPlayed(it) }
    )

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    init {
        loadInitialData()
        observeSessionChanges()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val contact = contactRepository.getContactById(chatId)
                val profile = profileRepository.getCurrentProfileSnapshot()

                _uiState.update {
                    it.copy(
                        title = contact?.displayName ?: profile?.nickname ?: "",
                        peerId = contact?.id,
                        peerAvatar = contact?.avatarPath,
                        myId = profile?.id,
                        myAvatar = profile?.avatarPath,
                        isMyself = contact == null
                    )
                }
            } catch (_: Exception) {
            }

            // 标记已读
            launch(Dispatchers.IO) {
                messageRepository.markAllAsRead(chatId)
            }
        }
    }

    private fun observeSessionChanges() {
        viewModelScope.launch {
            sessionFlow.collect { session ->
                session?.let { s ->
                    _uiState.update {
                        it.copy(
                            backgroundPath = s.backgroundPath,
                            isMuted = s.isMuted,
                            isOnline = s.isOnline
                        )
                    }
                }
            }
        }
    }

    fun sendMessage(content: MessageContent, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            messageRepository.sendMessage(
                sessionId = chatId,
                receiverId = chatId,
                content = content
            ).onSuccess {
                onSent()
            }.onFailure {
                _uiState.update { it.copy(isSending = false) }
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
        if (messages.value.size < PAGE_SIZE
            || _uiState.value.isLoadingMore
            || !_uiState.value.hasMoreMessages
        ) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            // 增加观察计数
            val newCount = _visibleCount.value + PAGE_SIZE
            _visibleCount.value = newCount

            // 查询是否还有更多数据
            val hasMore = checkHasMore()

            delay(1000)
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    hasMoreMessages = hasMore
                )
            }
        }
    }

    private suspend fun checkHasMore(): Boolean {
        val oldestTimestamp = messages.value.lastOrNull()?.timestamp ?: return false
        return messageRepository.hasOlderMessages(chatId, oldestTimestamp)
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
            messages.value.find { it.id == messageId }?.let { message ->
                if (message.content.showUnreadDot) {
                    viewModelScope.launch {
                        messageRepository.markVoiceAsPlayed(messageId)
                    }
                }
            }
        }
    }

    fun prepareRequestAddFriend() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.let { state ->
                val contact = Contact(
                    id = state.peerId!!,
                    nickname = state.title,
                    avatarPath = state.peerAvatar
                )
                contactP2PRepository.setContactToCache(
                    contactId = chatId,
                    contact = contact
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
    }
}

data class ChatSessionUiState(
    val title: String = "",
    val peerId: String? = null,
    val peerAvatar: String? = null,
    val myId: String? = null,
    val myAvatar: String? = null,
    val isMyself: Boolean = false,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val shouldScrollToBottom: Boolean = false,
    val backgroundPath: String? = null,
    val isMuted: Boolean = false,
    val isOnline: Boolean = false
)