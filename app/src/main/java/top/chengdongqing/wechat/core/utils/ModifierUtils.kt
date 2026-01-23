package top.chengdongqing.wechat.core.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import kotlin.math.roundToInt

/**
 * 自定义点击，不带水波纹
 */
fun Modifier.weClickable(
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/**
 * 自定义点击，带自定义背景色
 */
fun Modifier.weClickableWithBg(
    showBackground: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedColor = WeChatTheme.colorScheme.divider

    this
        .background(if (isPressed && showBackground) pressedColor else Color.Transparent)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * 长按连续触发
 *
 * @param enabled 是否启用
 * @param initialDelayMillis 长按判定时间
 * @param minDelayMillis 最快触发间隔
 * @param delayDecayMillis 每次触发后缩短的间隔时间（越按越快）
 * @param onClick 触发的回调
 */
fun Modifier.repeatingClickable(
    enabled: Boolean = true,
    initialDelayMillis: Long = 200L,
    minDelayMillis: Long = 50L,
    delayDecayMillis: Long = 10L,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)

    pointerInput(enabled) {
        if (!enabled) return@pointerInput

        detectTapGestures(
            onPress = {
                val job = scope.launch {
                    currentOnClick() // 按下立即触发一次
                    delay(initialDelayMillis)

                    var currentDelay = 150L
                    while (isActive) {
                        currentOnClick()
                        delay(currentDelay)
                        // 逐渐加速
                        if (currentDelay > minDelayMillis) {
                            currentDelay -= delayDecayMillis
                        }
                    }
                }
                try {
                    awaitRelease() // 等待手指抬起
                } finally {
                    job.cancel() // 停止循环
                }
            }
        )
    }
}

fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())