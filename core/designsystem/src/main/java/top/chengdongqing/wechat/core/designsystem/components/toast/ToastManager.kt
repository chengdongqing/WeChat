package top.chengdongqing.wechat.core.designsystem.components.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object ToastManager {
    private val _events = MutableSharedFlow<ToastEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun show(
        title: String,
        icon: ToastIcon = ToastIcon.None,
        duration: Duration = 1500.milliseconds,
        mask: Boolean = false
    ) {
        _events.tryEmit(ToastEvent.Show(title, icon, duration, mask))
    }

    fun success(title: String) = show(title, ToastIcon.Success)

    fun fail(title: String) = show(title, ToastIcon.Fail)

    fun loading(title: String) =
        show(title, ToastIcon.Loading, duration = Duration.INFINITE, mask = true)

    fun hide() {
        _events.tryEmit(ToastEvent.Hide)
    }
}

sealed interface ToastEvent {
    data class Show(
        val title: String,
        val icon: ToastIcon,
        val duration: Duration,
        val mask: Boolean
    ) : ToastEvent

    data object Hide : ToastEvent
}

@Composable
fun WeToastHost() {
    val state = rememberToastState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            ToastManager.events.collect { event ->
                when (event) {
                    is ToastEvent.Show -> {
                        state.show(event.title, event.icon, event.duration, event.mask)
                    }

                    else -> {
                        state.hide()
                    }
                }
            }
        }
    }
}