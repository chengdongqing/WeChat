package top.chengdongqing.wechat.ui.chat.session

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import top.chengdongqing.wechat.data.model.ChatMessage

/**
 * 当数据更新时，消息列表自动置底
 */
@Composable
fun MessageDataScrollEffect(
    listState: LazyListState,
    messages: List<ChatMessage>
) {
    val topMessageId = remember(messages.size) { derivedStateOf { messages.firstOrNull()?.id } }

    LaunchedEffect(topMessageId.value) {
        if (topMessageId.value != null) {
            if (listState.firstVisibleItemIndex <= 1) {
                listState.animateScrollToItem(0)
            }
        }
    }
}

/**
 * 当键盘弹出时，消息列表自动置底
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardScrollEffect(
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