package top.chengdongqing.wechat.core.designsystem.components.actionsheet

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

object ActionSheetManager {
    private val channel = Channel<ActionSheetRequest>(Channel.UNLIMITED)
    val requests = channel.receiveAsFlow()

    fun show(
        options: List<ActionSheetItem>,
        @StringRes title: Int? = null,
        onCancel: (() -> Unit)? = null,
        onAction: (index: Int) -> Unit
    ) {
        channel.trySend(
            ActionSheetRequest(
                options = options,
                title = title,
                onCancel = onCancel,
                onAction = onAction
            )
        )
    }
}

data class ActionSheetRequest(
    val options: List<ActionSheetItem>,
    @StringRes val title: Int?,
    val onCancel: (() -> Unit)? = null,
    val onAction: (index: Int) -> Unit
)

@Composable
fun WeActionSheetHost() {
    val state = rememberActionSheetState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { // 只让处于前台的Activity处理事件
            ActionSheetManager.requests.collect { request ->
                state.show(
                    options = request.options,
                    title = request.title,
                    onCancel = request.onCancel,
                    onAction = request.onAction
                )
                // 等本次弹窗关闭后再处理下一条请求
                snapshotFlow { state.visible }.first { !it }
            }
        }
    }
}