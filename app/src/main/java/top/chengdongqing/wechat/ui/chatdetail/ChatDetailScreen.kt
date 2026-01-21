package top.chengdongqing.wechat.ui.chatdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar

@Composable
fun ChatDetailScreen(friendId: String, onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            WeTopBar(title = "张三", onBack = onBack) {
                ActionIcon(iconResId = R.drawable.ic_more_outline, description = "更多")
            }
        },
        bottomBar = {
            ChatBottomBar(
                text = inputText,
                onTextChange = { inputText = it },
                isVoiceMode = isVoiceMode,
                onModeChange = { isVoiceMode = it },
                onSend = {
                    // 发送逻辑
                    inputText = ""
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF3F3F3)),
            contentPadding = PaddingValues(10.dp),
            reverseLayout = true // 新消息在底部，旧消息在顶部；键盘弹出时列表会自动推上去
        ) {
            items(20) { index ->
                ChatBubbleItem(isFromMe = index % 2 == 0, text = "消息内容 $index")
            }
        }
    }
}

/**
 * 注：
 * 在输入框聚焦的瞬间，系统试图通过“推挤”整个窗口（AdjustPan）来给键盘留位，
 * 而 Compose 又在尝试自己计算空间（AdjustResize），两者打起架来会导致页面剧烈闪动。
 * 解决方式：修改 AndroidManifest.xml，设置：android:windowSoftInputMode="adjustResize"
 */