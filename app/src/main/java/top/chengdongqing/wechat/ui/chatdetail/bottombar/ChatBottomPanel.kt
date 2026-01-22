package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.data.sticker.Emoji

@Composable
fun ExpandablePanel(
    inputMode: ChatInputMode,
    defaultHeight: Dp = 300.dp,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardHeight = with(density) { ime.getBottom(this).toDp() }
    val panelHeight = remember(keyboardHeight) {
        if (keyboardHeight > 0.dp) keyboardHeight else defaultHeight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(Color(0xFFF1F1F1))
    ) {
        when (inputMode) {
            ChatInputMode.EMOJI -> EmojiPanel(onEmojiSelect, onStickerSelect, onBackspace)
            ChatInputMode.MORE -> MorePanel()
            else -> Unit
        }
    }
}