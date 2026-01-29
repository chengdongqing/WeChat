package top.chengdongqing.wechat.ui.chat.session.input.text

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.chat.session.ActionIcon
import top.chengdongqing.wechat.ui.chat.session.CircleActionIcon
import top.chengdongqing.wechat.ui.chat.session.input.InputHandler
import top.chengdongqing.wechat.ui.chat.session.input.InputMode
import top.chengdongqing.wechat.ui.chat.session.input.InputPanelHolder
import top.chengdongqing.wechat.ui.chat.session.input.rememberInputModeController
import top.chengdongqing.wechat.ui.components.popup.WePopup
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester

@Composable
fun InputOverlay(
    visible: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit
) {
    WePopup(
        visible = visible,
        padding = PaddingValues.Zero,
        draggable = false,
        onClose = onClose
    ) {
        val focusRequester = remember { NativeFocusRequester() }
        val controller = rememberInputModeController(focusRequester)
        val inputMode by controller.inputMode
        val scope = rememberCoroutineScope()

        val currentText = rememberUpdatedState(text)
        val inputHandler = remember {
            InputHandler(currentText, focusRequester, scope, onTextChange)
        }

        BackHandler {
            if (inputMode.isEmoji) {
                controller.switchMode(showKeyboard = false)
            } else {
                onClose()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 顶部关闭按钮
            InputTopBar(onClose)

            // 全屏输入区域
            Column(
                modifier = Modifier
                    .zIndex(1f)
                    .weight(1f)
                    .background(Color.White)
            ) {
                EmojiTextField(
                    value = text,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    focusRequester = focusRequester,
                    maxHeightDp = null,
                    onValueChange = onTextChange
                )

                // 语音和表情快捷键
                InputActionBar(
                    isEmojiMode = inputMode.isEmoji,
                    onToggleMode = {
                        val targetMode = if (inputMode.isEmoji) InputMode.TEXT else InputMode.EMOJI
                        controller.switchMode(targetMode)
                    }
                )
            }

            // 面板容器
            InputPanelHolder(
                inputMode,
                isInPopup = true,
                onEmojiSelect = { inputHandler.insertEmoji(it.description) },
                onBackspace = { inputHandler.handleEmojiBackspace() }
            )
        }
    }
}

@Composable
private fun InputActionBar(isEmojiMode: Boolean, onToggleMode: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        // 语音输入按键
        ActionIcon(iconResId = R.drawable.ic_mic_outlined) {}

        Spacer(Modifier.width(8.dp))

        // 切换表情按键
        ActionIcon(
            iconResId = if (isEmojiMode) {
                R.drawable.ic_keyboard_outlined
            } else {
                R.drawable.ic_emoji_outlined
            },
            onClick = onToggleMode
        )
    }
}

@Composable
private fun InputTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        CircleActionIcon(
            iconResId = R.drawable.ic_arrow_down_outlined,
            onClick = onClose
        )
    }
}