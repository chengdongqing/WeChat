package top.chengdongqing.wechat.feature.moments.ui.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalMomentsSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalMomentsAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.momentsMediaSharedElement(momentId: String, imageIndex: Int): Modifier {
    val sharedScope = LocalMomentsSharedTransitionScope.current ?: return this
    val visibilityScope = LocalMomentsAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(
                "moments-media-$momentId-$imageIndex"
            ),
            animatedVisibilityScope = visibilityScope,
            resizeMode = ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(2.dp))
        )
    }
}
