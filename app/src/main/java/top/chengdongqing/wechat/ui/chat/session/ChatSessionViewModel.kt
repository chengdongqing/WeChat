package top.chengdongqing.wechat.ui.chat.session

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.MessageContent
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ChatSessionState(
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = ""
)

class ChatSessionViewModel(
//    private val friendId: String,
//    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatSessionState())
    val state = _state.asStateFlow()

    init {
        loadInitialMessages()
    }

    private fun loadInitialMessages() {
        _state.update { it.copy(messages = generateMockChatData(), title = "张三") }
    }

    fun updateText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentText = _state.value.inputText
        if (currentText.isBlank()) return

        val newMessage = ChatMessage(
            id = randomUUID(),
            content = MessageContent.Text(currentText),
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )

        _state.update {
            it.copy(
                messages = listOf(newMessage) + it.messages,
                inputText = ""
            )
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