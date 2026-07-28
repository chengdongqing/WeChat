package top.chengdongqing.wechat.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.chengdongqing.wechat.core.model.CallType

/**
 * Cross-feature contract for starting a call after the requesting screen has
 * handled microphone/camera permissions.
 */
interface CallLauncher {
    @Composable
    fun rememberLauncher(peerId: String): (CallType) -> Unit
}

val LocalCallLauncher = staticCompositionLocalOf<CallLauncher> {
    object : CallLauncher {
        @Composable
        override fun rememberLauncher(peerId: String): (CallType) -> Unit = {
            error("CallLauncher is not provided by the app host")
        }
    }
}
