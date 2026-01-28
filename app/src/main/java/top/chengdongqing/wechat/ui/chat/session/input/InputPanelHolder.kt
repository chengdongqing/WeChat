package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.utils.rememberKeyboardHeight
import top.chengdongqing.wechat.data.emoji.Emoji
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.input.panels.EmojiPanel
import top.chengdongqing.wechat.ui.chat.session.input.panels.MoreAction
import top.chengdongqing.wechat.ui.chat.session.input.panels.MoreActionPanel

@Composable
fun InputPanelHolder(
    inputMode: InputMode,
    isInPopup: Boolean = false,
    recentEmojis: List<Emoji> = emptyList(),
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: ((MessageContent.Sticker) -> Unit)? = null,
    onBackspace: () -> Unit,
    onAction: ((MoreAction) -> Unit)? = null
) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime

    var savedKeyboardHeight by rememberSaveable(stateSaver = DpSaver) {
        mutableStateOf(InputPanelConfig.DEFAULT_PANEL_HEIGHT)
    }

    val keyboardHeight = rememberKeyboardHeight()

    // 最小高度限制，避免过小的键盘高度
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > savedKeyboardHeight.coerceAtLeast(InputPanelConfig.MIN_PANEL_HEIGHT)) {
            savedKeyboardHeight = keyboardHeight
        }
    }

    val panelHeight = calculatePanelHeight(
        inputMode = inputMode,
        isInPopup = isInPopup,
        keyboardHeight = keyboardHeight,
        savedKeyboardHeight = savedKeyboardHeight,
        imeHeight = with(density) { ime.getBottom(density).toDp() }
    )

    val animatedPanelHeight by animateDpAsState(
        targetValue = panelHeight,
        animationSpec = tween(),
        label = "PanelHeightAnimation"
    )

    // 只在需要显示面板时才渲染
    if (inputMode != InputMode.TEXT || animatedPanelHeight > 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedPanelHeight)
                .background(Color(0xFFF1F1F1))
                .clipToBounds() // 防止内容溢出
        ) {
            when (inputMode) {
                InputMode.EMOJI -> EmojiPanel(
                    emojiOnly = isInPopup,
                    recentEmojis = recentEmojis,
                    onEmojiSelect = onEmojiSelect,
                    onStickerSelect = onStickerSelect,
                    onBackspace = onBackspace
                )

                InputMode.MORE -> MoreActionPanel(
                    onAction = onAction ?: {}
                )
            }
        }
    }
}

/**
 * 计算面板高度
 */
private fun calculatePanelHeight(
    inputMode: InputMode,
    isInPopup: Boolean,
    keyboardHeight: Dp,
    savedKeyboardHeight: Dp,
    imeHeight: Dp
): Dp {
    return when {
        inputMode.isText -> if (isInPopup) {
            imeHeight
        } else {
            keyboardHeight
        }

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

private val DpSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp }
)