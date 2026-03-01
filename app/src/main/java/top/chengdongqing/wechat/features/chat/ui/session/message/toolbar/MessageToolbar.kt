package top.chengdongqing.wechat.features.chat.ui.session.message.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction

/**
 * 消息操作工具条
 */
@Composable
fun MessageToolbar(
    visible: Boolean,
    actions: List<MessageAction>,
    position: Offset,
    bubblePosition: Offset,
    bubbleHeight: Float,
    isTextMessage: Boolean,
    onActionClick: (MessageAction) -> Unit,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var shouldShowPopup by remember { mutableStateOf(false) }

    /**
     * 缓存参数，防止关闭时跳动
     */
    var cachedPosition by remember { mutableStateOf(Offset.Zero) }
    var cachedBubblePosition by remember { mutableStateOf(Offset.Zero) }
    var cachedBubbleHeight by remember { mutableFloatStateOf(0f) }
    var cachedIsTextMessage by remember { mutableStateOf(false) }
    var cachedActions by remember { mutableStateOf<List<MessageAction>>(emptyList()) }

    /**
     * 工具条实际高度（动态测量）
     */
    var toolbarActualHeight by remember { mutableFloatStateOf(0f) }

    /**
     * 控制显示/隐藏动画
     */
    LaunchedEffect(visible) {
        if (visible) {
            cachedPosition = position
            cachedBubblePosition = bubblePosition
            cachedBubbleHeight = bubbleHeight
            cachedIsTextMessage = isTextMessage
            cachedActions = actions

            shouldShowPopup = true
            delay(50)
            showContent = true
        } else {
            showContent = false
            delay(200)
            shouldShowPopup = false
        }
    }

    if (!shouldShowPopup) return

    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    /**
     * 计算行数和预估高度
     */
    val rows = remember(cachedActions) {
        cachedActions.chunked(5)
    }

    /**
     * 预估高度：每行约62dp(16*2+24+4+2) + 分隔线1dp
     */
    val estimatedHeight = with(density) {
        (rows.size * 62.dp + (rows.size - 1) * 1.dp).toPx()
    }

    /**
     * 使用实际高度或预估高度
     */
    val effectiveHeight = if (toolbarActualHeight > 0) toolbarActualHeight else estimatedHeight

    /**
     * 判断是否显示在下方
     */
    val showBelow = cachedBubblePosition.y < effectiveHeight + with(density) { 20.dp.toPx() }

    /**
     * 计算工具条宽度（5列或实际列数）
     */
    val toolbarWidth = with(density) {
        (minOf(cachedActions.size, 5) * 60.dp).toPx()
    }

    /**
     * X轴位置：居中对齐，考虑屏幕边界
     */
    val toolbarX = (cachedPosition.x - toolbarWidth / 2)
        .coerceIn(10f, containerSize.width - toolbarWidth - 10f)

    /**
     * Y轴位置：根据消息类型和显示位置计算
     */
    val toolbarY = if (cachedIsTextMessage) {
        // 文本消息：跟随手指位置
        if (showBelow) {
            cachedPosition.y + 20
        } else {
            cachedPosition.y - effectiveHeight - 10
        }
    } else {
        // 其他消息：固定在气泡上方/下方
        if (showBelow) {
            cachedBubblePosition.y + cachedBubbleHeight + 10
        } else {
            cachedBubblePosition.y - effectiveHeight - 10
        }
    }

    val offset = IntOffset(toolbarX.toInt(), toolbarY.toInt())
    val transformOrigin = TransformOrigin(0.5f, if (showBelow) 0f else 1f)

    Popup(
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true
        )
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 200)
            ) + scaleIn(
                initialScale = 0.8f,
                transformOrigin = transformOrigin,
                animationSpec = tween(durationMillis = 200)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 150)
            ) + scaleOut(
                targetScale = 0.8f,
                transformOrigin = transformOrigin,
                animationSpec = tween(durationMillis = 150)
            )
        ) {
            ActionButtonGroup(
                actions = cachedActions,
                onActionClick = {
                    onActionClick(it)
                    onDismiss()
                },
                onHeightMeasured = { height ->
                    toolbarActualHeight = height
                }
            )
        }
    }
}

/**
 * 操作按钮组
 * 支持多行显示（每行最多5个）
 */
@Composable
private fun ActionButtonGroup(
    actions: List<MessageAction>,
    onActionClick: (MessageAction) -> Unit,
    onHeightMeasured: (Float) -> Unit
) {
    val rows = remember(actions) {
        actions.chunked(5)
    }

    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF525252))
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            }
    ) {
        rows.forEachIndexed { rowIndex, rowActions ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowActions.forEach { action ->
                    ActionButton(
                        action = action,
                        onClick = {
                            onActionClick(action)
                        }
                    )
                }
            }

            if (rowIndex < rows.lastIndex) {
                WeDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

/**
 * 单个操作按钮
 */
@Composable
private fun ActionButton(
    action: MessageAction,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = action.label,
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .padding(bottom = 4.dp)
        )
        Text(
            text = action.label,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}