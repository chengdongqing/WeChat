package top.chengdongqing.wechat.core.designsystem.components.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.R

/**
 * 加载中动画图标
 *
 * @param size 大小
 * @param color 颜色
 * @param isRunning 是否旋转
 */
@Composable
fun WeLoading(
    size: Dp = 16.dp,
    color: Color = Color.Unspecified,
    isRunning: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "WeLoadingAnimation"
    )

    Icon(
        painter = painterResource(id = R.drawable.ic_loading),
        contentDescription = "loading",
        modifier = Modifier
            .size(size)
            .rotate(angle),
        tint = color
    )
}