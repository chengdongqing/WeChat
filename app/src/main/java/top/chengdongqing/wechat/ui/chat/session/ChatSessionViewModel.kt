package top.chengdongqing.wechat.ui.chat.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
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
        .map { msgs -> msgs.mapNotNull { it.content as? MessageContent.Media } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
}