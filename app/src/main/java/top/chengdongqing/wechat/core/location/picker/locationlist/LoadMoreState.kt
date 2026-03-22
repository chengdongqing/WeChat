package top.chengdongqing.wechat.core.location.picker.locationlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
interface LoadMoreState {
    /** 是否在加载更多 */
    val isLoadingMore: Boolean

    /** 嵌套滚动协调器，挂载到列表容器上 */
    val nestedScrollConnection: NestedScrollConnection
}

@Composable
fun rememberLoadMoreState(
    enabled: () -> Boolean = { true },
    onReachBottom: suspend () -> Unit
): LoadMoreState {
    val coroutineScope = rememberCoroutineScope()
    return remember { LoadMoreStateImpl(enabled, onReachBottom, coroutineScope) }
}

private class LoadMoreStateImpl(
    private val enabled: () -> Boolean,
    private val onReachBottom: suspend () -> Unit,
    private val coroutineScope: CoroutineScope
) : LoadMoreState {
    override var isLoadingMore by mutableStateOf(false)

    override val nestedScrollConnection = object : NestedScrollConnection {
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            // 向上 fling 越界、未在加载中、且外部允许时才触发，防止重复并发请求
            if (available.y < 0 && enabled() && !isLoadingMore) {
                coroutineScope.launch {
                    isLoadingMore = true
                    onReachBottom()
                    isLoadingMore = false
                }
                return available
            }
            return Velocity.Zero
        }
    }
}
