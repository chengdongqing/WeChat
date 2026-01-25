package top.chengdongqing.wechat.ui.chat.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.chat.session.input.InputBar
import top.chengdongqing.wechat.ui.chat.session.message.MessageItem
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.utils.BounceOverscrollEffect

@Composable
fun ChatSessionScreen(
    friendId: String,
    viewModel: ChatSessionViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = state.messages

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember { BounceOverscrollEffect(scope) }

    // 键盘弹出时自动滚动到最新的消息
    KeyboardScrollEffect(listState, messages.size)
    // 当消息列表数据更新时，自动置底
    MessageDataScrollEffect(listState, messages)

    Scaffold(
        topBar = {
            WeTopBar(title = "张三", onBack = onBack) {
                ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多")
            }
        },
        bottomBar = {
            InputBar(
                text = state.inputText,
                onTextChange = { viewModel.updateText(it) },
                onSend = { viewModel.sendMessage() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF3F3F3))
                .overscroll(overscrollEffect),
            contentPadding = PaddingValues(10.dp),
            reverseLayout = true, // 新消息在底部，旧消息在顶部；键盘弹出时列表会自动推上去
            verticalArrangement = Arrangement.Top,
            overscrollEffect = overscrollEffect
        ) {
            itemsIndexed(
                items = messages,
                key = { _, message -> message.id }
            ) { index, message ->
                MessageItem(message)
                TimeDivider(messages, index)
            }
        }
    }
}