package top.chengdongqing.wechat.features.chat.ui.session.input.panel

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.designsystem.model.Emoji
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.DpSaver
import top.chengdongqing.wechat.core.util.rememberKeyboardHeight
import top.chengdongqing.wechat.features.chat.domain.model.InputBarActions
import top.chengdongqing.wechat.features.chat.domain.model.InputMode

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

    // 最小高度限制，避免键盘高度过小
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > savedKeyboardHeight.coerceAtLeast(InputPanelConfig.MIN_PANEL_HEIGHT)) {
            savedKeyboardHeight = keyboardHeight
        }
    }

    val panelHeight = calculatePanelHeight(
        inputMode = inputMode,
        keyboardHeight = keyboardHeight,
        savedKeyboardHeight = savedKeyboardHeight
    )

    val animatedPanelHeight by animateDpAsState(
        targetValue = panelHeight,
        animationSpec = tween(),
        label = "PanelHeightAnimation"
    )

    // 只在需要显示面板时才渲染
    if (inputMode != InputMode.Text || animatedPanelHeight > 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedPanelHeight)
                .background(WeTheme.colorScheme.background)
                .clipToBounds() // 防止内容溢出
        ) {
            when (inputMode) {
                InputMode.Emoji -> EmojiPanel(
                    recentEmojis = recentEmojis,
                    onEmojiSelect = { emoji -> actions.onInsertEmoji(emoji.description) },
                    onStickerSelect = if (!isInPopup) actions.onSendMessage else null,
                    onBackspace = actions.onEmojiBackspace
                )

                InputMode.More -> MoreActionPanel(actions.onMoreAction)

                else -> {}
            }
        }
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
}