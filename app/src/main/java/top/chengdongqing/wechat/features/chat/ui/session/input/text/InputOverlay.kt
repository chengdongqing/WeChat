package top.chengdongqing.wechat.features.chat.ui.session.input.text

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.EmojiTextField
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.ui.session.components.ActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.components.CircleActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.input.InputMode
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.InputPanelHolder
import top.chengdongqing.wechat.features.chat.ui.session.input.rememberInputBarController

@Composable
fun InputOverlay(
    visible: Boolean, inputText: String, onTextChange: (String) -> Unit, onClose: () -> Unit
) {
    WePopup(
        visible = visible, padding = PaddingValues.Zero, draggable = false, onClose = onClose
    ) {
        val focusRequester = remember { NativeFocusRequester() }
        val controller = rememberInputBarController(focusRequester)
        val state by controller.state.collectAsStateWithLifecycle()
        val inputMode = state.inputMode

        LaunchedEffect(inputText) {
            controller.updateText(inputText)
        }

        BackHandler {
            if (inputMode.isEmoji) {
                controller.switchMode(showKeyboard = false)
            } else {
                onTextChange(state.inputText)
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
                    value = state.inputText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    focusRequester = focusRequester,
                    maxHeightDp = null,
                    onValueChange = controller::updateText
                )

                // 语音和表情快捷键
                InputActionBar(
                    isEmojiMode = inputMode.isEmoji,
                    onSpeechInput = { text ->
                        controller.updateText(
                            state.inputText.let { if (it.isNotEmpty()) "$it，" else it } + text
                        )
                    },
                    onToggleMode = {
                        val targetMode = if (inputMode.isEmoji) InputMode.Text else InputMode.Emoji
                        controller.switchMode(targetMode)
                    }
                )
            }

            // 面板容器
            InputPanelHolder(
                inputMode,
                onEmojiSelect = { controller.insertEmoji(it.description) },
                onBackspace = { controller.handleEmojiBackspace() })
        }
    }
}

@Composable
private fun InputActionBar(
    isEmojiMode: Boolean, onSpeechInput: (String) -> Unit, onToggleMode: () -> Unit
) {
    val speechState = rememberSpeechInputState(onSpeechInput)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp), horizontalArrangement = Arrangement.End
    ) {
        // 语音输入按键
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (speechState.isListening) WeTheme.colorScheme.primary else Color.Unspecified)
        ) {
            ActionIcon(
                iconResId = R.drawable.ic_mic_circle_outlined,
                tint = if (speechState.isListening) Color.White else WeTheme.colorScheme.textPrimary,
                description = "语音输入"
            ) {
                speechState.toggle()
            }
        }

        Spacer(Modifier.width(8.dp))

        // 切换表情按键
        ActionIcon(
            iconResId = if (isEmojiMode) {
                R.drawable.ic_keyboard_outlined
            } else {
                R.drawable.ic_emoji_outlined
            }, description = "切换面板", onClick = onToggleMode
        )
    }
}

@Composable
private fun InputTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), horizontalArrangement = Arrangement.Start
    ) {
        CircleActionIcon(
            iconResId = R.drawable.ic_arrow_down_outlined, onClick = onClose
        )
    }
}