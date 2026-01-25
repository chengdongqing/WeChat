package top.chengdongqing.wechat.ui.chat.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.formatChatTime
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.ui.chat.session.input.InputBar
import top.chengdongqing.wechat.ui.chat.session.message.MessageItem
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.utils.BounceOverscrollEffect
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Composable
fun ChatSessionScreen(friendId: String, onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember { BounceOverscrollEffect(scope) }
    val chatMessages = remember { generateMockChatData() }

    // 键盘弹出时自动滚动到最后的消息
    LaunchedEffectKeyboardScroll(listState, chatMessages.size)

    Scaffold(
        topBar = {
            WeTopBar(title = "张三", onBack = onBack) {
                ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多")
            }
        },
        bottomBar = {
            InputBar(
                text = inputText,
                onTextChange = { inputText = it },
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
                .background(Color(0xFFF3F3F3))
                .overscroll(overscrollEffect),
            contentPadding = PaddingValues(10.dp),
            reverseLayout = true, // 新消息在底部，旧消息在顶部；键盘弹出时列表会自动推上去
            verticalArrangement = Arrangement.Top,
            overscrollEffect = overscrollEffect
        ) {
            itemsIndexed(
                items = chatMessages,
                key = { _, message -> message.id }) { index, message ->
                MessageItem(message)
                TimeDivider(chatMessages, index)
            }
        }
    }
}

@Composable
private fun TimeDivider(
    chatMessages: List<ChatMessage>,
    index: Int
) {
    val message = chatMessages[index]

    // 是否显示时间
    val shouldShow by remember(index) {
        derivedStateOf {
            // 在 reverseLayout 中，index 最大的那条是时间轴上的第一条（最旧的消息）
            if (index == chatMessages.size - 1) {
                true
            } else {
                // index + 1 是逻辑上的上一条消息（更旧的那条）
                val prevMessage = chatMessages[index + 1]
                message.timestamp - prevMessage.timestamp > 5 * 60 * 1000
            }
        }
    }

    if (shouldShow) {
        val displayTime = remember(message.timestamp) {
            formatChatTime(message.timestamp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayTime,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LaunchedEffectKeyboardScroll(
    listState: LazyListState,
    itemCount: Int
) {
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && itemCount > 0) {
            listState.scrollToItem(0)
        }
    }
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
                    text = mockTexts.random(),
                    timestamp = timestamp,
                    isFromMe = (0..1).random() == 1
                )
            )
        }
    }

    return messages.sortedByDescending { it.timestamp }
}