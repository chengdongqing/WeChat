package top.chengdongqing.wechat.ui.chat.list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.Chat

data class ChatListState(
    val chats: List<Chat> = emptyList(),
)

class ChatListViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        _state.update { it.copy(chats = generateMockChats()) }
    }

    /**
     * 标为已读/未读
     */
    fun toggleReadStatus(index: Int) {
        _state.update { state ->
            val chat = state.chats.getOrNull(index) ?: return@update state
            val newList = state.chats.toMutableList()
            val newUnreadCount = if (chat.unreadCount > 0) 0 else 1
            newList[index] = chat.copy(unreadCount = newUnreadCount)
            state.copy(chats = newList)
        }
    }

    /**
     * 聊天置顶
     */
    fun stickToTop(index: Int) {}

    /**
     * 隐藏聊天
     */
    fun hideChat(index: Int) {}

    /**
     * 删除聊天
     */
    fun deleteChat(index: Int) {}

    private fun generateMockChats(count: Int = 100): List<Chat> {
        val names = listOf("张三", "李四", "王五", "Compose 交流群", "文件传输助手", "GitHub 通知")
        val messages = listOf("好的", "吃了没？", "[图片]", "有人在吗？", "代码已提交", "明天见")

        return List(count) { i ->
            Chat(
                id = randomUUID(),
                name = "${names[i % names.size]} $i",
                lastMessage = messages[i % messages.size],
                time = "${12}:${(10 + i % 50).toString().padStart(2, '0')}",
                avatarRes = 0,
                unreadCount = if (i % 9 == 0) i * 2 else 0
            )
        }
    }
}