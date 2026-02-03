package top.chengdongqing.wechat.features.chat.ui.list

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.Chat
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor() : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        _chats.update { generateMockChats() }
    }

    /**
     * 标为已读/未读
     */
    fun toggleReadStatus(index: Int) {
        _chats.update { chats ->
            val chat = chats.getOrNull(index) ?: return
            val newList = chats.toMutableList()
            val newUnreadCount = if (chat.unreadCount > 0) 0 else 1
            newList[index] = chat.copy(unreadCount = newUnreadCount)
            newList
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