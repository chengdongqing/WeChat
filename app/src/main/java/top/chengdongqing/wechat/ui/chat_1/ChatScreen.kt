package top.chengdongqing.wechat.ui.chat_1

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.data.model.WifiLanPeer
import top.chengdongqing.wechat.ui.call_1.CallActivity
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
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val currentPeer = remember(peers, peerId) { peers.find { it.id == peerId } }

    // 2. 观察来自 Repository 的所有消息，并过滤出与该 Peer 的对话
    val allMessages by viewModel.messages.collectAsStateWithLifecycle(emptyList())
    val chatMessages = remember(allMessages, peerId) {
        allMessages.filter { it.chatId == peerId }
    }

    val context = LocalContext.current
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
                        viewModel.sendMedia(currentPeer, uri)
                    }
                },
                onVideoCall = {
                    if (currentPeer != null) {
                        startCall(context, (currentPeer as WifiLanPeer).ip)
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

private fun startCall(context: Context, targetIp: String) {
    val intent = Intent(context, CallActivity::class.java).apply {
        // 传递必要参数
        putExtra("targetIp", targetIp)
        putExtra("isOfferer", true) // 标记你是拨打方

        // 如果是从后台跳转（比如收到推送/信令），建议加上这两个 Flag
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    context.startActivity(intent)
}