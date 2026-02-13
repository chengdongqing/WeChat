package top.chengdongqing.wechat.features.chat.ui.session.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage

/**
 * 加载更多监听
 */
@Composable
fun LoadMoreEffect(
    listState: LazyListState,
    messages: List<ChatMessage>,
    isLoadingMore: Boolean,
    hasMoreMessages: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, messages, isLoadingMore, hasMoreMessages) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex == null || isLoadingMore || !hasMoreMessages) return@collect

            // 当滚动到倒数第3个item时触发加载
            val threshold = messages.size - 3
            if (lastVisibleIndex >= threshold && messages.isNotEmpty()) {
                onLoadMore()
            }
        }
    }
}