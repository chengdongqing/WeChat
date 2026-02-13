package top.chengdongqing.wechat.features.chat.ui.session.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 生命周期感知的语音播放控制
 */
@Composable
fun VoicePlayingLifecycle(onVoiceStop: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(Unit) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onVoiceStop()
            }
        }
        lifecycle.addObserver(lifecycleObserver)

        onDispose {
            onVoiceStop()
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}
