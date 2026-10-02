package top.chengdongqing.wechat.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * 页面转场动画
 */
object NavTransitions {

    private const val DURATION_MS = 350

    private val EmphasizedEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    private val IntOffsetAnimationSpec = tween<IntOffset>(
        durationMillis = DURATION_MS,
        easing = EmphasizedEasing
    )

    private val FloatAnimationSpec = tween<Float>(
        durationMillis = DURATION_MS,
        easing = EmphasizedEasing
    )

    /**
     * 前进转场 (Push / Enter)
     * - 新页面：从右侧 100% 平移滑入
     * - 旧页面：向左平移 30% (视差 parallax) 配合轻微淡出
     */
    val Enter: ContentTransform = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = IntOffsetAnimationSpec
    ) togetherWith (
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                animationSpec = IntOffsetAnimationSpec
            ) + fadeOut(
                targetAlpha = 0.8f,
                animationSpec = FloatAnimationSpec
            )
            )

    /**
     * 返回转场 (Pop / Exit)
     * - 顶层页面：向右 100% 滑出
     * - 底层页面：从左侧 -30% 平移恢复到原位 + 恢复透明度
     */
    val Exit: ContentTransform = (
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                animationSpec = IntOffsetAnimationSpec
            ) + fadeIn(
                initialAlpha = 0.8f,
                animationSpec = FloatAnimationSpec
            )
            ) togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = IntOffsetAnimationSpec
    )
}