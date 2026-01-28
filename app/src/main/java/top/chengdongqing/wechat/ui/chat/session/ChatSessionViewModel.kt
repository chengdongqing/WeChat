package top.chengdongqing.wechat.ui.chat.session

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.media.VoicePlayer
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.utils.EmojiRenderer
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ChatSessionState(
    val title: String = "",
    val isSending: Boolean = false
)

class ChatSessionViewModel(
//    private val friendId: String,
//    private val repository: ChatRepository
) : ViewModel() {
    // 数据层：消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    // UI状态层
    private val _uiState = MutableStateFlow(ChatSessionState())
    val uiState = _uiState.asStateFlow()

    // 派生状态：媒体列表（缓存）
    val mediaList: StateFlow<List<MessageContent.Media>> = messages
        .map { messages -> messages.mapNotNull { it.content as? MessageContent.Media }.reversed() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val voicePlayer = VoicePlayer()
    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId = _playingMessageId.asStateFlow()

    init {
        loadInitialMessages()
    }

    private fun loadInitialMessages() {
        _messages.value = generateMockChatData()
        _uiState.update { it.copy(title = "张三") }
    }

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

    fun finishScrollToLatest() {
        _uiState.update {
            it.copy(isSending = false)
        }
    }

    fun loadMore(lastMsgId: String) {

    }

    private fun generateMockChatData(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val now = Instant.now()

        // 定义需要测试的时间跨度
        val testBuckets = listOf(
            // 数量 | 时间偏移量
            5 to Duration.ofMinutes(2),    // 刚刚 (几分钟前)
            3 to Duration.ofHours(2),      // 今天更早 (几小时前)
            5 to Duration.ofDays(1),       // 昨天
            5 to Duration.ofDays(3),       // 本周内 (周几)
            5 to Duration.ofDays(10),      // 今年稍早 (几月几日)
            5 to Duration.ofDays(400)      // 往年
        )

        val mockTexts = listOf(
            "你好！",
            "在干嘛？",
            "OK",
            "看下这个时间对吗？",
            "这是一条长消息用来测试最大宽度限制是否正常。"
        )

        testBuckets.forEach { (count, duration) ->
            repeat(count) {
                // 在对应时间区间内再加一点随机偏移，防止所有消息时间戳完全一样
                val randomOffset = Duration.ofMinutes((1..60).random().toLong())
                val timestamp = now.minus(duration).minus(randomOffset).toEpochMilli()

                messages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = MessageContent.Text(mockTexts.random()),
                        timestamp = timestamp,
                        isFromMe = (0..1).random() == 1
                    )
                )
            }
        }

        return messages.sortedByDescending { it.timestamp }
    }

    fun toggleVoicePlay(messageId: String, uri: Uri) {
        if (_playingMessageId.value == messageId) {
            stopVoice()
        } else {
            startPlaying(messageId, uri)
        }
    }

    private fun startPlaying(messageId: String, uri: Uri) {
        _playingMessageId.value = messageId
        markAsPlayed(messageId)

        voicePlayer.play(uri) {
            // 播放完成后尝试自动播放下一条
            playNextUnreadVoice(messageId)
        }
    }

    /**
     * 更新已读状态
     */
    private fun markAsPlayed(messageId: String) {
        _messages.update { currentList ->
            val index = currentList.indexOfFirst { it.id == messageId }
            if (index == -1) return@update currentList

            val targetMsg = currentList[index]
            val content = targetMsg.content

            if (content is MessageContent.Voice && !content.isPlayed) {
                val newList = currentList.toMutableList()
                val newContent = content.copy(isPlayed = true)
                newList[index] = targetMsg.copy(content = newContent)
                newList
            } else {
                currentList
            }
        }
    }

    /**
     * 寻找下一条未播放的语音
     */
    private fun playNextUnreadVoice(currentMsgId: String) {
        val currentList = _messages.value
        // 找到当前消息的索引
        val currentIndex = currentList.indexOfFirst { it.id == currentMsgId }

        if (currentIndex != -1) {
            // 从当前消息开始，寻找更晚发出的语音（索引减小方向），index 越小，消息越新
            for (i in (currentIndex - 1) downTo 0) {
                val nextMsg = currentList[i]
                val content = nextMsg.content

                // 必须是语音消息，且从未播放过
                if (content is MessageContent.Voice && !content.isPlayed) {
                    viewModelScope.launch {
                        _playingMessageId.value = null
                        delay(250)
                        startPlaying(nextMsg.id, content.uri)
                    }
                    return // 找到并开始播放后，直接退出循环
                }
            }
        }

        // 如果循环结束没找到满足条件的，清空播放状态
        _playingMessageId.value = null
    }

    fun stopVoice() {
        voicePlayer.stop()
        _playingMessageId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        EmojiRenderer.clearCache()
    }
}