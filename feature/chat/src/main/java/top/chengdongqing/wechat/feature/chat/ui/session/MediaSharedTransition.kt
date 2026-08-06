package top.chengdongqing.wechat.feature.chat.ui.session

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalMediaSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalMediaAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.mediaSharedElement(messageId: String): Modifier {
    val sharedScope = LocalMediaSharedTransitionScope.current ?: return this
    val visibilityScope = LocalMediaAnimatedVisibilityScope.current ?: return this

    return with(sharedScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState("chat-media-$messageId"),
            animatedVisibilityScope = visibilityScope,
            resizeMode = ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(6.dp))
        )
    }
}
