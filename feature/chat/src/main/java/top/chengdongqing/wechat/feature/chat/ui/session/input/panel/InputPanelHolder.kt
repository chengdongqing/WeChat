package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.compose.DpSaver
import top.chengdongqing.wechat.core.designsystem.model.Emoji
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.window.rememberKeyboardHeight
import top.chengdongqing.wechat.feature.chat.domain.model.InputMode
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBarActions

@Composable
fun InputPanelHolder(
    inputMode: InputMode,
    actions: InputBarActions,
    isInPopup: Boolean = false,
    recentEmojis: List<Emoji> = emptyList()
) {
    val keyboardHeight = rememberKeyboardHeight()
    var savedKeyboardHeight by rememberSaveable(stateSaver = DpSaver) {
        mutableStateOf(InputPanelConfig.DEFAULT_PANEL_HEIGHT)
    }
    var expressionPanelExtraHeight by remember {
        mutableStateOf(0.dp)
    }
    var isResizingExpressionPanel by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val screenHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }

    // 最小高度限制，避免键盘高度过小
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > savedKeyboardHeight.coerceAtLeast(InputPanelConfig.MIN_PANEL_HEIGHT)) {
            savedKeyboardHeight = keyboardHeight
        }
    }

    // 每次重新打开表情面板都从默认高度开始，不保留上一次的展开状态。
    LaunchedEffect(inputMode) {
        if (!inputMode.isEmoji) {
            expressionPanelExtraHeight = 0.dp
            isResizingExpressionPanel = false
        }
    }

    val basePanelHeight = calculatePanelHeight(
        inputMode = inputMode,
        keyboardHeight = keyboardHeight,
        savedKeyboardHeight = savedKeyboardHeight
    )
    val maxExpressionPanelHeight = (screenHeight * InputPanelConfig.MAX_SCREEN_HEIGHT_FRACTION)
        .coerceAtLeast(basePanelHeight)
    val maxExpressionPanelExtraHeight =
        (maxExpressionPanelHeight - basePanelHeight).coerceAtLeast(0.dp)
    val expressionResizeDragState = rememberDraggableState { dragAmount ->
        val heightDelta = with(density) { (-dragAmount).toDp() }
        expressionPanelExtraHeight = (expressionPanelExtraHeight + heightDelta)
            .coerceIn(0.dp, maxExpressionPanelExtraHeight)
    }
    val panelHeight = if (inputMode.isEmoji && !isInPopup) {
        (basePanelHeight + expressionPanelExtraHeight)
            .coerceIn(basePanelHeight, maxExpressionPanelHeight)
    } else {
        basePanelHeight
    }

    val animatedPanelHeight by animateDpAsState(
        targetValue = panelHeight,
        animationSpec = if (isResizingExpressionPanel) snap() else tween(),
        label = "PanelHeightAnimation"
    )

    // 只在需要显示面板时才渲染
    if (inputMode != InputMode.Text || animatedPanelHeight > 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedPanelHeight)
                .background(WeTheme.colorScheme.background)
                .clipToBounds()
        ) {
            when (inputMode) {
                InputMode.Emoji -> ExpressionPanel(
                    recentEmojis = recentEmojis,
                    onEmojiSelect = { emoji -> actions.onInsertEmoji(emoji.description) },
                    onStickerSelect = if (!isInPopup) actions.onSendMessage else null,
                    onBackspace = actions.onEmojiBackspace,
                    resizeHandle = if (isInPopup) null else {
                        {
                            ExpressionPanelResizeHandle(
                                modifier = Modifier.draggable(
                                    state = expressionResizeDragState,
                                    orientation = Orientation.Vertical,
                                    onDragStarted = { isResizingExpressionPanel = true },
                                    onDragStopped = { velocity ->
                                        val velocityThreshold = with(density) {
                                            InputPanelConfig.FLING_VELOCITY_THRESHOLD.toPx()
                                        }
                                        expressionPanelExtraHeight = when {
                                            velocity <= -velocityThreshold ->
                                                maxExpressionPanelExtraHeight

                                            velocity >= velocityThreshold -> 0.dp
                                            expressionPanelExtraHeight >=
                                                    maxExpressionPanelExtraHeight / 2 ->
                                                maxExpressionPanelExtraHeight

                                            else -> 0.dp
                                        }
                                        isResizingExpressionPanel = false
                                    }
                                )
                            )
                        }
                    }
                )

                InputMode.More -> MoreActionPanel(actions.onMoreAction)

                else -> {}
            }
        }
    }
}

@Composable
private fun ExpressionPanelResizeHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(InputPanelConfig.RESIZE_HANDLE_TOUCH_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = InputPanelConfig.RESIZE_HANDLE_WIDTH,
                    height = InputPanelConfig.RESIZE_HANDLE_HEIGHT
                )
                .clip(RoundedCornerShape(50))
                .background(WeTheme.colorScheme.divider)
        )
    }
}

/**
 * 计算面板高度
 */
private fun calculatePanelHeight(
    inputMode: InputMode,
    keyboardHeight: Dp,
    savedKeyboardHeight: Dp
): Dp {
    return when {
        inputMode.isText -> keyboardHeight

        inputMode.isEmoji -> (savedKeyboardHeight + InputPanelConfig.EMOJI_PANEL_EXTRA_HEIGHT)
            .coerceAtLeast(InputPanelConfig.MIN_PANEL_HEIGHT)

        inputMode.isMore -> savedKeyboardHeight
            .coerceAtLeast(InputPanelConfig.MIN_PANEL_HEIGHT)

        else -> 0.dp
    }
}

private object InputPanelConfig {
    /** 默认面板高度 */
    val DEFAULT_PANEL_HEIGHT = 300.dp

    /** 最小面板高度 */
    val MIN_PANEL_HEIGHT = 200.dp

    /** 表情面板额外高度 */
    val EMOJI_PANEL_EXTRA_HEIGHT = 20.dp

    /** 拖高后仍为消息列表保留足够空间 */
    const val MAX_SCREEN_HEIGHT_FRACTION = 0.7f

    val RESIZE_HANDLE_TOUCH_HEIGHT = 32.dp
    val RESIZE_HANDLE_WIDTH = 40.dp
    val RESIZE_HANDLE_HEIGHT = 4.dp

    /** 超过该速度时按甩动方向切换高度，不再要求越过距离中点。 */
    val FLING_VELOCITY_THRESHOLD = 1000.dp
}
