package top.chengdongqing.wechat.core.designsystem.util

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

/**
 * 自定义点击，不带水波纹
 */
fun Modifier.weClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * 自定义点击，带自定义背景色
 */
fun Modifier.weClickableWithBg(
    enabled: Boolean = true,
    showBackground: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedColor = WeTheme.colorScheme.divider

    this
        .background(if (isPressed && showBackground) pressedColor else Color.Transparent)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
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
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    this
        .indication(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        )
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)

                    val job = scope.launch {
                        currentOnClick() // 按下立即触发一次
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // 轻微震动
                        delay(initialDelayMillis)

                        var currentDelay = initialDelayMillis
                        while (isActive) {
                            currentOnClick()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // 连续震动

                            delay(currentDelay)
                            // 逐渐加速
                            if (currentDelay > minDelayMillis) {
                                currentDelay -= delayDecayMillis
                            }
                        }
                    }

                    try {
                        // 等待手指抬起
                        val released = tryAwaitRelease()
                        // 隐藏波纹
                        if (released) {
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    } finally {
                        job.cancel() // 停止循环
                    }
                }
            )
        }
}

/**
 * 支持虚线边框
 */
fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    shape: Shape = RectangleShape,
    dashWidth: Dp = 6.dp,
    dashGap: Dp = 4.dp
) = this.drawWithContent {
    drawContent()

    val strokeWidth = width.toPx()
    val dashOn = dashWidth.toPx()
    val dashOff = dashGap.toPx()

    val outline = shape.createOutline(size, layoutDirection, this)

    drawOutline(
        outline = outline,
        color = color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f)
        )
    )
}

fun Boolean?.isTrue(): Boolean = this == true