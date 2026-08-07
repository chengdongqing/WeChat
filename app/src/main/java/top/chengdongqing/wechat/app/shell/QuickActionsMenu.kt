package top.chengdongqing.wechat.app.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.Gray
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.model.AppLanguage

/**
 * 快捷操作枚举
 */
enum class QuickAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    GroupChat(R.drawable.ic_chats_filled, R.string.home_action_new_group),
    AddFriend(R.drawable.ic_add_friends_filled, R.string.home_action_add_friend),
    Scan(R.drawable.ic_scan_filled, R.string.home_action_scan),
    Payment(R.drawable.ic_pay_vendor_filled, R.string.home_action_payment)
}

@Composable
fun QuickActionsMenu(
    visibleState: MutableTransitionState<Boolean>,
    anchorPosition: Offset,
    anchorSize: IntSize,
    onDismiss: () -> Unit,
    onAction: (QuickAction) -> Unit
) {
    val menuWidth = when (LocalAppearanceSetting.current.appLanguage) {
        AppLanguage.English -> 180.dp
        else -> 160.dp
    }
    val density = LocalDensity.current
    val popupOffset = remember(density, anchorPosition, anchorSize) {
        with(density) {
            val menuWidthPx = menuWidth.roundToPx()
            val x = anchorPosition.x.toInt() + (anchorSize.width) - menuWidthPx
            val y = anchorPosition.y.toInt() + anchorSize.height + 4.dp.roundToPx()
            IntOffset(x, y)
        }
    }

    if (visibleState.currentState || visibleState.targetState) {
        Popup(
            offset = popupOffset,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            val pivotX = 0.9f
            val pivotY = 0f
            val animationSpec = tween<Float>(durationMillis = 150, easing = LinearOutSlowInEasing)

            AnimatedVisibility(
                visibleState = visibleState,
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
                Column(
                    modifier = Modifier
                        .width(menuWidth)
                        .padding(top = 8.dp)
                        .drawMenuArrow(Gray)
                        .background(Gray, RoundedCornerShape(4.dp))
                ) {
                    val actions = QuickAction.entries
                    actions.forEachIndexed { index, action ->
                        ActionItem(action) {
                            onDismiss()
                            onAction(action)
                        }

                        if (index < actions.lastIndex) {
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
}

@Composable
private fun ActionItem(action: QuickAction, onClick: () -> Unit) {
    val color = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(action.label),
            color = color,
            fontSize = 16.sp
        )
    }
}

private fun Modifier.drawMenuArrow(backgroundColor: Color): Modifier = this.drawBehind {
    val sizePx = 14.dp.toPx()
    val arrowOff = 16.dp.toPx()
    val tx = size.width - arrowOff

    rotate(degrees = 45f, pivot = Offset(tx, -sizePx / 2)) {
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset(tx - sizePx / 2, 0f),
            size = Size(sizePx, sizePx),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}
