package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.core.utils.rememberKeyboardHeight
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.ui.chat.session.input.panels.EmojiPanel
import top.chengdongqing.wechat.ui.chat.session.input.panels.MoreActionPanel

@Composable
fun InputPanelHolder(
    inputMode: InputMode,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keyboardHeight = rememberKeyboardHeight()
    var savedKeyboardHeight by remember { mutableStateOf(300.dp) }

    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0.dp && savedKeyboardHeight == 300.dp) {
            savedKeyboardHeight = keyboardHeight
        }
    }

    // 最终占位高度
    val panelHeight = when {
        inputMode.isText -> keyboardHeight
        inputMode.isEmoji -> savedKeyboardHeight + 20.dp
        inputMode.isMore -> savedKeyboardHeight
        else -> 0.dp
    }

    val animatedPanelHeight by animateDpAsState(
        targetValue = panelHeight,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "SmoothSwitch"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedPanelHeight)
            .background(Color(0xFFF1F1F1))
    ) {
        when (inputMode) {
            InputMode.EMOJI -> EmojiPanel(onEmojiSelect, onStickerSelect, onBackspace)
            InputMode.MORE -> MoreActionPanel()
        }
    }
}