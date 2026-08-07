package top.chengdongqing.wechat.core.designsystem.components.dialog

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.theme.LinkBlue

object DialogManager {
    private val channel = Channel<DialogRequest>(Channel.UNLIMITED)
    val requests = channel.receiveAsFlow()

    fun show(
        title: String,
        content: String? = null,
        @StringRes okText: Int = R.string.action_ok,
        @StringRes cancelText: Int = R.string.action_cancel,
        okColor: Color = LinkBlue,
        closeOnAction: Boolean = true,
        onCancel: (() -> Unit)? = {},
        onOk: (() -> Unit)? = null
    ) {
        channel.trySend(
            DialogRequest(
                title,
                content,
                okText,
                cancelText,
                okColor,
                closeOnAction,
                onCancel,
                onOk
            )
        )
    }
}

data class DialogRequest(
    val title: String,
    val content: String?,
    @StringRes val okText: Int,
    @StringRes val cancelText: Int,
    val okColor: Color,
    val closeOnAction: Boolean,
    val onCancel: (() -> Unit)?,
    val onOk: (() -> Unit)?
)

@Composable
fun WeDialogHost() {
    val state = rememberDialogState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            DialogManager.requests.collect { request ->
                state.show(
                    title = request.title,
                    content = request.content,
                    okText = request.okText,
                    cancelText = request.cancelText,
                    okColor = request.okColor,
                    closeOnAction = request.closeOnAction,
                    onCancel = request.onCancel,
                    onOk = request.onOk
                )
                // 等本次弹窗关闭后再处理下一条请求
                snapshotFlow { state.visible }.first { !it }
            }
        }
    }
}