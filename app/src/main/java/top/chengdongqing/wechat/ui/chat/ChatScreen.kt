package top.chengdongqing.wechat.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.ui.components.ChatInputBar
import top.chengdongqing.wechat.ui.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    peerId: String,
    onBack: () -> Unit
) {
    // 1. 获取当前聊天的对象信息（用于标题栏显示名字）
    val peers by viewModel.nearbyPeers.collectAsStateWithLifecycle()
    val currentPeer = remember(peers, peerId) { peers.find { it.id == peerId } }

    // 2. 观察来自 Repository 的所有消息，并过滤出与该 Peer 的对话
    val allMessages by viewModel.messages.collectAsStateWithLifecycle(emptyList())
    val chatMessages = remember(allMessages, peerId) {
        allMessages//.filter { it.chatId == peerId }
    }

    val listState = rememberLazyListState()

    // 自动滚动到最新消息
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.scrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPeer?.name ?: "正在聊天...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                onTextSend = { text ->
                    if (currentPeer != null) {
                        viewModel.sendText(currentPeer, text)
                    }
                },
                onImageSelected = { uri ->
                    if (currentPeer != null) {
                        viewModel.sendImage(currentPeer, uri)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
    }
}