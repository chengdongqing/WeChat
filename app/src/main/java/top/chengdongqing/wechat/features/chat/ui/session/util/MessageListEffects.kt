package top.chengdongqing.wechat.features.chat.ui.session.util

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import kotlin.math.abs

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

/**
 * 当键盘弹出或更多面板显示时，用户手动滚动列表，实现自动收起键盘或面板
 * 但是由于收到了消息或者自己发出了消息，列表自动滚动到最底部导致的滚动，不处理
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollToDismissEffect(
    listState: LazyListState,
    isPanelMode: Boolean,
    thresholdPx: Int = 100,
    onDismiss: () -> Unit
) {
    val currentIsImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    val currentIsPanelMode by rememberUpdatedState(isPanelMode)

    // 监测用户的手势交互状态
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(listState, isUserDragging) {
        // 如果用户没有手在屏幕上拖拽，直接跳过逻辑
        if (!isUserDragging) return@LaunchedEffect

        var totalDelta = 0f
        var lastOffset: Int? = null

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (_, currentOffset) ->
                if (lastOffset != null) {
                    // 这里简化计算：只针对单次滑动的累加
                    // 如果 Index 变了，说明滑过了一个 Item，直接判定超过阈值
                    val delta = abs(currentOffset - lastOffset!!)
                    totalDelta += delta

                    if (totalDelta > thresholdPx) {
                        if (currentIsImeVisible || currentIsPanelMode) {
                            onDismiss()
                        }
                    }
                }
                lastOffset = currentOffset
            }
    }
}