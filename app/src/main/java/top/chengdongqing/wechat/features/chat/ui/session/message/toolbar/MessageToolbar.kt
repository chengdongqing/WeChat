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
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction

private const val ANIM_ENTER_MS = 200
private const val ANIM_EXIT_MS = 150
private const val ITEMS_PER_ROW = 5
private const val ITEM_WIDTH_DP = 60
private const val SCREEN_MARGIN_PX = 10f

/**
 * 工具条位置参数（缓存用，避免关闭动画期间参数跳动）
 */
@Immutable
private data class CachedToolbarParams(
    val bubblePosition: Offset = Offset.Zero,
    val bubbleHeight: Float = 0f,
    val isTextMessage: Boolean = false,
    val actions: List<MessageAction> = emptyList()
)

/**
 * 消息操作工具条
 */
@Composable
fun MessageToolbar(
    visible: Boolean,
    actions: List<MessageAction>,
    bubblePosition: Offset,
    bubbleHeight: Float,
    isTextMessage: Boolean,
    onActionClick: (MessageAction) -> Unit,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var shouldShowPopup by remember { mutableStateOf(false) }
    var cached by remember { mutableStateOf(CachedToolbarParams()) }
    var measuredHeight by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            cached = CachedToolbarParams(
                bubblePosition = bubblePosition,
                bubbleHeight = bubbleHeight,
                isTextMessage = isTextMessage,
                actions = actions
            )
            shouldShowPopup = true
            delay(50)
            showContent = true
        } else {
            showContent = false
            delay(ANIM_EXIT_MS.toLong() + 50)
            shouldShowPopup = false
        }
    }

    if (!shouldShowPopup) return

    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize

    val position = remember(cached, measuredHeight, containerSize) {
        computeToolbarPosition(
            params = cached,
            measuredHeight = measuredHeight,
            screenWidth = containerSize.width.toFloat(),
            density = density.density
        )
    }

    Popup(
        offset = position.offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(ANIM_ENTER_MS)) + scaleIn(
                initialScale = 0.8f,
                transformOrigin = position.transformOrigin,
                animationSpec = tween(ANIM_ENTER_MS)
            ),
            exit = fadeOut(tween(ANIM_EXIT_MS)) + scaleOut(
                targetScale = 0.8f,
                transformOrigin = position.transformOrigin,
                animationSpec = tween(ANIM_EXIT_MS)
            )
        ) {
            ActionButtonGroup(
                actions = cached.actions,
                onActionClick = {
                    onActionClick(it)
                    onDismiss()
                },
                onHeightMeasured = { measuredHeight = it }
            )
        }
    }
}

/**
 * 工具条定位结果
 */
@Immutable
private data class ToolbarPosition(
    val offset: IntOffset,
    val transformOrigin: TransformOrigin
)

/**
 * 计算工具条显示位置
 *
 * 文本消息跟随气泡位置，其他消息固定在气泡上方或下方。
 * 屏幕顶部空间不足时自动切换到下方显示。
 */
private fun computeToolbarPosition(
    params: CachedToolbarParams,
    measuredHeight: Float,
    screenWidth: Float,
    density: Float
): ToolbarPosition {
    val rows = (params.actions.size + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW
    val rowHeightPx = 62f * density
    val dividerPx = 1f * density
    val estimatedHeight = rows * rowHeightPx + (rows - 1) * dividerPx
    val effectiveHeight = if (measuredHeight > 0f) measuredHeight else estimatedHeight

    val toolbarWidth = minOf(params.actions.size, ITEMS_PER_ROW) * ITEM_WIDTH_DP * density
    val margin = 20f * density
    val gap = 10f * density

    val showBelow = params.bubblePosition.y < effectiveHeight + margin

    val x = (params.bubblePosition.x - toolbarWidth / 2f)
        .coerceIn(SCREEN_MARGIN_PX, screenWidth - toolbarWidth - SCREEN_MARGIN_PX)

    val y = if (params.isTextMessage) {
        if (showBelow) params.bubblePosition.y + margin
        else params.bubblePosition.y - effectiveHeight - gap
    } else {
        if (showBelow) params.bubblePosition.y + params.bubbleHeight + gap
        else params.bubblePosition.y - effectiveHeight - gap
    }

    val origin = TransformOrigin(0.5f, if (showBelow) 0f else 1f)

    return ToolbarPosition(
        offset = IntOffset(x.toInt(), y.toInt()),
        transformOrigin = origin
    )
}

/**
 * 操作按钮组
 *
 * 每行最多5个按钮，行间用分隔线隔开
 */
@Composable
private fun ActionButtonGroup(
    actions: List<MessageAction>,
    onActionClick: (MessageAction) -> Unit,
    onHeightMeasured: (Float) -> Unit
) {
    val rows = remember(actions) { actions.chunked(ITEMS_PER_ROW) }

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
                    ActionButton(action) {
                        onActionClick(action)
                    }
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
            contentDescription = stringResource(action.labelRes),
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .padding(bottom = 4.dp)
        )
        Text(
            text = stringResource(action.labelRes),
            color = Color.White,
            fontSize = 11.sp
        )
    }
}