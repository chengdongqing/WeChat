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
import top.chengdongqing.wechat.features.chat.domain.model.InputMode
import top.chengdongqing.wechat.features.chat.ui.session.components.ActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.components.CircleActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBarActions
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBarController
import top.chengdongqing.wechat.features.chat.ui.session.input.InputBarState
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.InputPanelHolder
import top.chengdongqing.wechat.features.chat.ui.session.input.rememberInputBarController

@Composable
fun InputOverlay(
    state: InputBarState,
    actions: InputBarActions
) {
    val onClose = actions.onToggleExpand

    WePopup(
        draggable = false,
        visible = state.isExpanded,
        padding = PaddingValues.Zero,
        onClose = onClose
    ) {
        val focusRequester = remember { NativeFocusRequester() }
        val controller = rememberInputBarController(focusRequester)
        val innerState by controller.state.collectAsStateWithLifecycle()
        val inputMode = innerState.inputMode
        val innerActions = rememberInputBarActions(controller)

        // 同步外部文本数据
        LaunchedEffect(state.inputText) {
            controller.updateText(state.inputText)
        }

        // 处理系统返回
        BackHandler {
            if (inputMode.isEmoji) {
                controller.switchMode(showKeyboard = false)
            } else {
                actions.onTextChange(innerState.inputText)
                onClose()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部关闭按钮
            InputTopBar(onClose)

            // 全屏输入区域
            Column(
                modifier = Modifier
                    .zIndex(1f)
                    .weight(1f)
            ) {
                EmojiTextField(
                    value = innerState.inputText,
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
                            innerState.inputText.let { if (it.isNotEmpty()) "$it，" else it } + text
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
                inputMode = inputMode,
                actions = innerActions,
                recentEmojis = innerState.recentEmojis,
                isInPopup = true
            )
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
                icon = R.drawable.ic_mic_circle_outlined,
                tint = if (speechState.isListening) Color.White else WeTheme.colorScheme.textPrimary,
                description = "语音输入"
            ) {
                speechState.toggle()
            }
        }

        Spacer(Modifier.width(8.dp))

        // 切换表情按键
        ActionIcon(
            icon = if (isEmojiMode) {
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
            icon = R.drawable.ic_arrow_down_outlined, onClick = onClose
        )
    }
}

@Composable
fun rememberInputBarActions(
    controller: InputBarController
): InputBarActions {
    return remember(controller) {
        InputBarActions(
            onInsertEmoji = controller::insertEmoji,
            onEmojiBackspace = controller::handleEmojiBackspace,
        )
    }
}