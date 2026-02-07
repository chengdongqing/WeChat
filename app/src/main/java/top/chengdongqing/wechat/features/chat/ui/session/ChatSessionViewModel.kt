package top.chengdongqing.wechat.features.chat.ui.session

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.util.EmojiRenderer
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.media.VoicePlayer
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.input.voice.AudioFocusManager
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ChatSessionState(
    val title: String = "",
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true
)

@HiltViewModel(assistedFactory = ChatSessionViewModel.Factory::class)
class ChatSessionViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val soundTipPlayer: SoundTipPlayer,
    @ApplicationContext context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chatId: String): ChatSessionViewModel
    }

    // 数据层
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _uiState = MutableStateFlow(ChatSessionState())
    val uiState = _uiState.asStateFlow()

    // 派生状态：媒体列表
    val mediaList: StateFlow<List<MessageContent.Media>> = messages
        .map { messages ->
            messages.mapNotNull { it.content as? MessageContent.Media }.reversed()
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
    val playingMessageId = _playingMessageId.asStateFlow()

    // 分页加载配置
    private var currentPage = 0
    private val pageSize = 20
    private var oldestTimestamp: Long? = null

    init {
        loadInitialMessages()
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            val initialMessages = generateMockChatData(page = 0, pageSize = pageSize)
            _messages.value = initialMessages
            oldestTimestamp = initialMessages.lastOrNull()?.timestamp
            currentPage = 1

            _uiState.update {
                it.copy(
                    title = "张三",
                    hasMoreMessages = initialMessages.size >= pageSize
                )
            }
        }
    }

    /**
     * 发送消息
     */
    fun sendMessage(content: MessageContent, onSent: () -> Unit) {
        _uiState.update { it.copy(isSending = true) }

        val newMessage = ChatMessage(
            id = randomUUID(),
            content = content,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )

        _messages.update { listOf(newMessage) + it }
        onSent()
    }

    /**
     * 完成滚动到最新消息
     */
    fun finishScrollToLatest() {
        _uiState.update { it.copy(isSending = false) }
    }

    /**
     * 加载更多历史消息
     */
    fun loadMore(lastVisibleMsgId: String) {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMoreMessages) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            // 模拟网络延迟
            delay(500)

            try {
                val moreMessages = generateMockChatData(
                    page = currentPage,
                    pageSize = pageSize,
                    beforeTimestamp = oldestTimestamp
                )

                if (moreMessages.isNotEmpty()) {
                    _messages.update { current -> current + moreMessages }
                    oldestTimestamp = moreMessages.lastOrNull()?.timestamp
                    currentPage++

                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            hasMoreMessages = moreMessages.size >= pageSize
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            hasMoreMessages = false
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    /**
     * 生成模拟数据
     */
    private fun generateMockChatData(
        page: Int,
        pageSize: Int,
        beforeTimestamp: Long? = null
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val baseTime = beforeTimestamp?.let { Instant.ofEpochMilli(it) } ?: Instant.now()

        val mockTexts = listOf(
            "你好！",
            "在干嘛？",
            "OK",
            "看下这个时间对吗？",
            "这是一条长消息用来测试最大宽度限制是否正常。",
            "收到",
            "好的，没问题",
            "待会见",
            "忙完了吗？"
        )

        // 根据页码生成不同时间范围的消息
        val startOffset = when (page) {
            0 -> Duration.ofMinutes(5)
            1 -> Duration.ofDays(1)
            2 -> Duration.ofDays(3)
            3 -> Duration.ofDays(7)
            4 -> Duration.ofDays(14)
            else -> Duration.ofDays(30L * (page - 4))
        }

        repeat(pageSize) { index ->
            val randomOffset = Duration.ofMinutes((index * 30L) + (1..30).random().toLong())
            val timestamp = baseTime.minus(startOffset).minus(randomOffset).toEpochMilli()

            messages.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = MessageContent.Text(mockTexts.random()),
                    timestamp = timestamp,
                    isFromMe = (0..1).random() == 1
                )
            )
        }

        return messages.sortedByDescending { it.timestamp }
    }

    /**
     * 切换语音播放状态
     */
    fun toggleVoicePlay(messageId: String, uri: Uri) {
        audioPlaybackManager.togglePlay(messageId, uri, _messages.value)
    }

    /**
     * 停止语音播放
     */
    fun stopVoice() {
        audioPlaybackManager.stop()
    }

    /**
     * 标记消息为已播放
     */
    private fun markAsPlayed(messageId: String) {
        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    val content = message.content
                    if (content is MessageContent.Voice && !content.isPlayed) {
                        message.copy(content = content.copy(isPlayed = true))
                    } else {
                        message
                    }
                } else {
                    message
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.release()
        EmojiRenderer.clearCache()
    }
}

/**
 * 音频播放管理器 - 封装所有音频播放相关逻辑
 */
private class AudioPlaybackManager(
    context: Context,
    private val soundTipPlayer: SoundTipPlayer,
    private val onPlayingStateChanged: (String?) -> Unit,
    private val onMessagePlayed: (String) -> Unit
) {
    private val audioFocusManager = AudioFocusManager(context)
    private val voicePlayer = VoicePlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentPlayingId: String? = null

    fun togglePlay(messageId: String, uri: Uri, messages: List<ChatMessage>) {
        if (currentPlayingId == messageId) {
            stop()
        } else {
            startPlaying(messageId, uri, messages, isContinuous = false)
        }
    }

    fun stop() {
        voicePlayer.stop()
        audioFocusManager.abandonFocus()
        currentPlayingId = null
        onPlayingStateChanged(null)
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun startPlaying(
        messageId: String,
        uri: Uri,
        messages: List<ChatMessage>,
        isContinuous: Boolean
    ) {
        // 首次播放时申请音频焦点
        if (!isContinuous) {
            audioFocusManager.requestFocus()
        }

        currentPlayingId = messageId
        onPlayingStateChanged(messageId)
        onMessagePlayed(messageId)

        voicePlayer.play(uri) {
            handlePlaybackCompleted(messageId, messages)
        }
    }

    private fun handlePlaybackCompleted(messageId: String, messages: List<ChatMessage>) {
        soundTipPlayer.play(R.raw.play_completed)

        val nextVoice = findNextUnreadVoice(messageId, messages)
        if (nextVoice != null) {
            // 连续播放下一条
            scope.launch {
                onPlayingStateChanged(null)
                delay(250)
                startPlaying(
                    messageId = nextVoice.id,
                    uri = nextVoice.uri,
                    messages = messages,
                    isContinuous = true
                )
            }
        } else {
            // 没有更多消息，释放音频焦点
            audioFocusManager.abandonFocus()
            currentPlayingId = null
            onPlayingStateChanged(null)
        }
    }

    private fun findNextUnreadVoice(
        currentMsgId: String,
        messages: List<ChatMessage>
    ): VoiceInfo? {
        val currentIndex = messages.indexOfFirst { it.id == currentMsgId }
        if (currentIndex == -1) return null

        // 从当前消息向更新的消息查找（index 越小，消息越新）
        for (i in (currentIndex - 1) downTo 0) {
            val message = messages[i]
            val content = message.content
            if (content is MessageContent.Voice && !content.isPlayed) {
                return VoiceInfo(message.id, content.uri)
            }
        }
        return null
    }

    private data class VoiceInfo(val id: String, val uri: Uri)
}