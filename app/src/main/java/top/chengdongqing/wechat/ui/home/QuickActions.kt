package top.chengdongqing.wechat.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.theme.Grey_4C
import top.chengdongqing.wechat.ui.utils.weClickableWithBg

data class MenuItem(
    @get:DrawableRes val iconResId: Int,
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun QuickActions(
    expanded: Boolean,
    items: List<MenuItem>,
    anchorPosition: Offset,
    anchorSize: IntSize,
    onDismissRequest: () -> Unit
) {
    var shouldShow by remember { mutableStateOf(expanded) }
    var isVisible by remember { mutableStateOf(false) }

    // 同步外部状态
    LaunchedEffect(expanded) {
        if (expanded) {
            shouldShow = true
            delay(10) // 极短延迟确保 Popup 已挂载后再播动画
            isVisible = true
        } else {
            isVisible = false
        }
    }

    if (!shouldShow) return

    val menuWidth = 160.dp
    val density = LocalDensity.current
    val popupOffset = remember(density, anchorPosition, anchorSize) {
        with(density) {
            val menuWidthPx = menuWidth.roundToPx()
            val x = anchorPosition.x.toInt() + (anchorSize.width) - menuWidthPx
            val y = anchorPosition.y.toInt() + anchorSize.height + 4.dp.roundToPx()
            IntOffset(x, y)
        }
    }

    Popup(
        offset = popupOffset,
        onDismissRequest = { isVisible = false },
        properties = PopupProperties(focusable = true)
    ) {
        val pivotX = 0.9f
        val pivotY = 0f
        val animationSpec = tween<Float>(durationMillis = 150, easing = LinearOutSlowInEasing)

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(pivotX, pivotY),
                animationSpec = tween(180, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec),
            exit = scaleOut(
                targetScale = 0.8f,
                transformOrigin = TransformOrigin(pivotX, pivotY),
                animationSpec = tween(150)
            ) + fadeOut(animationSpec)
        ) {
            DisposableEffect(Unit) {
                onDispose {
                    if (!isVisible) {
                        shouldShow = false
                        onDismissRequest()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(menuWidth)
                    .padding(top = 8.dp)
                    .drawMenuArrow()
                    .background(Grey_4C, RoundedCornerShape(4.dp))
            ) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .weClickableWithBg {
                                item.onClick()
                                onDismissRequest()
                            }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(item.iconResId),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.text,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    if (index < items.lastIndex) {
                        WeDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.drawMenuArrow(): Modifier = this.drawBehind {
    val sizePx = 14.dp.toPx()
    val arrowOff = 16.dp.toPx()
    val tx = size.width - arrowOff

    rotate(degrees = 45f, pivot = Offset(tx, -sizePx / 2)) {
        drawRoundRect(
            color = Grey_4C,
            topLeft = Offset(tx - sizePx / 2, 0f),
            size = Size(sizePx, sizePx),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}