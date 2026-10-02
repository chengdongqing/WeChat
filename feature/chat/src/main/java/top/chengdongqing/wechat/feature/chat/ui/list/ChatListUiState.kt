package top.chengdongqing.wechat.feature.chat.ui.list

import androidx.compose.runtime.Immutable
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.model.ChatSession

@Immutable
data class ChatListUiState(
    val chats: List<ChatSession> = emptyList(),
    val connectionMode: ConnectionMode = ConnectionMode.WiFiLan
)