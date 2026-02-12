package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage

/**
 * 当数据更新时，消息列表自动置底
 */
@Composable
fun MessageDataScrollEffect(
    listState: LazyListState,
    messages: List<ChatMessage>
) {
    val latestMessageId = messages.firstOrNull()?.id

    LaunchedEffect(latestMessageId) {
        if (latestMessageId != null && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollToDismissEffect(
    listState: LazyListState,
    isSending: Boolean,
    isPanelMode: Boolean,
    onDismiss: () -> Unit
) {
    val currentIsImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    val currentIsSending by rememberUpdatedState(isSending)
    val currentIsPanelMode by rememberUpdatedState(isPanelMode)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                delay(100)
                if (isScrolling && !currentIsSending) {
                    if (currentIsImeVisible || currentIsPanelMode) {
                        onDismiss()
                    }
                }
            }
    }
}