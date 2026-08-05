package top.chengdongqing.wechat.feature.chat.ui.session.message.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Path
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
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageAction
import kotlin.time.Duration.Companion.milliseconds

private const val ANIM_ENTER_MS = 200
private const val ANIM_EXIT_MS = 150
private const val ITEMS_PER_ROW = 5
private const val ITEM_WIDTH_DP = 60
private const val SCREEN_MARGIN_PX = 10f
private const val ARROW_WIDTH_DP = 12
private const val ARROW_HEIGHT_DP = 6
private const val ARROW_GAP_DP = 4

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
    temporarilyHidden: Boolean = false,
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
            shouldShowPopup = true
            delay(50.milliseconds)
            showContent = true
        } else {
            showContent = false
            delay((ANIM_EXIT_MS.toLong() + 50).milliseconds)
            shouldShowPopup = false
        }
    }

    LaunchedEffect(visible, bubblePosition, bubbleHeight, isTextMessage, actions) {
        if (visible) {
            cached = CachedToolbarParams(
                bubblePosition = bubblePosition,
                bubbleHeight = bubbleHeight,
                isTextMessage = isTextMessage,
                actions = actions
            )
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
        properties = PopupProperties(
            focusable = !cached.isTextMessage,
            dismissOnClickOutside = !cached.isTextMessage
        )
    ) {
        AnimatedVisibility(
            visible = showContent && !temporarilyHidden,
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
                showBelow = position.showBelow,
                arrowCenterX = position.arrowCenterX,
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
    val transformOrigin: TransformOrigin,
    val showBelow: Boolean,
    val arrowCenterX: Float
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
    val arrowHeightPx = ARROW_HEIGHT_DP * density
    val estimatedHeight = rows * rowHeightPx + (rows - 1) * dividerPx + arrowHeightPx
    val effectiveHeight = if (measuredHeight > 0f) measuredHeight else estimatedHeight

    val toolbarWidth = minOf(params.actions.size, ITEMS_PER_ROW) * ITEM_WIDTH_DP * density
    val margin = 20f * density
    val arrowGap = ARROW_GAP_DP * density

    val showBelow = params.bubblePosition.y < effectiveHeight + margin

    val x = (params.bubblePosition.x - toolbarWidth / 2f)
        .coerceIn(SCREEN_MARGIN_PX, screenWidth - toolbarWidth - SCREEN_MARGIN_PX)

    val y = if (showBelow) {
        params.bubblePosition.y + params.bubbleHeight + arrowGap
    } else {
        params.bubblePosition.y - effectiveHeight - arrowGap
    }

    val arrowMargin = (ARROW_WIDTH_DP / 2f + 4f) * density
    val arrowCenterX = (params.bubblePosition.x - x)
        .coerceIn(arrowMargin, toolbarWidth - arrowMargin)
    val origin = TransformOrigin(
        pivotFractionX = arrowCenterX / toolbarWidth,
        pivotFractionY = if (showBelow) 0f else 1f
    )

    return ToolbarPosition(
        offset = IntOffset(x.toInt(), y.toInt()),
        transformOrigin = origin,
        showBelow = showBelow,
        arrowCenterX = arrowCenterX
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
    showBelow: Boolean,
    arrowCenterX: Float,
    onActionClick: (MessageAction) -> Unit,
    onHeightMeasured: (Float) -> Unit
) {
    val rows = remember(actions) { actions.chunked(ITEMS_PER_ROW) }

    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .onGloballyPositioned { coordinates ->
                onHeightMeasured(coordinates.size.height.toFloat())
            }
    ) {
        if (showBelow) {
            ToolbarArrow(
                pointsUp = true,
                centerX = arrowCenterX
            )
        }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF525252))
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

        if (!showBelow) {
            ToolbarArrow(
                pointsUp = false,
                centerX = arrowCenterX
            )
        }
    }
}

@Composable
private fun ToolbarArrow(
    pointsUp: Boolean,
    centerX: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ARROW_HEIGHT_DP.dp)
    ) {
        val halfWidth = ARROW_WIDTH_DP.dp.toPx() / 2f
        val tipRadius = 1.5.dp.toPx()
        val path = Path().apply {
            if (pointsUp) {
                moveTo(centerX - tipRadius, tipRadius)
                quadraticTo(centerX, 0f, centerX + tipRadius, tipRadius)
                lineTo(centerX + halfWidth, size.height)
                lineTo(centerX - halfWidth, size.height)
            } else {
                moveTo(centerX - halfWidth, 0f)
                lineTo(centerX + halfWidth, 0f)
                lineTo(centerX + tipRadius, size.height - tipRadius)
                quadraticTo(
                    centerX,
                    size.height,
                    centerX - tipRadius,
                    size.height - tipRadius
                )
            }
            close()
        }
        drawPath(path, Color(0xFF525252))
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
