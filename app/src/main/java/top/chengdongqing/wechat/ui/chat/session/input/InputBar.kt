package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.chat.session.ActionIcon
import top.chengdongqing.wechat.ui.chat.session.CircleActionIcon
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester
import top.chengdongqing.wechat.ui.utils.rememberToggleState

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { NativeFocusRequester() }
    val controller = rememberInputModeController(focusRequester)
    val inputMode by controller.inputMode
    val scope = rememberCoroutineScope()

    // 当前输入框行数
    var lineCount by remember { mutableIntStateOf(1) }
    // 是否启用全屏输入
    val (isFullscreenText, toggleFullscreenText) = rememberToggleState(
        defaultValue = false,
        reverseValue = true
    )

    val currentText = rememberUpdatedState(text)
    val inputManger = remember {
        InputManger(currentText, onTextChange, focusRequester, scope)
    }

    Column(
        modifier = Modifier
            .background(Color(0xFFF7F7F7))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 语音/文字切换
            VoiceButton(
                showExpandButton = lineCount >= 3,
                inputMode,
                controller,
                focusRequester,
                onFullscreenText = { toggleFullscreenText() }
            )
            // 输入框区域
            InputBox(
                text = text,
                inputMode = inputMode,
                focusRequester = focusRequester,
                onTextChange = onTextChange,
                onLineCountChange = { lineCount = it }
            )
            // 表情按钮
            EmojiButton(inputMode, controller)
            // 发送/更多按钮
            SendOrMoreButton(text, onSend, inputMode, controller)
        }

        InputPanelHolder(
            inputMode,
            onEmojiSelect = { inputManger.insertEmoji(it.description) },
            onStickerSelect = {},
            onBackspace = { inputManger.handleEmojiBackspace() }
        )
    }

    // 全屏输入框
    FullScreenInputPopup(
        visible = isFullscreenText.value,
        text = text,
        onTextChange = onTextChange,
        onClose = { toggleFullscreenText() }
    )
}

@Composable
private fun VoiceButton(
    showExpandButton: Boolean = false,
    inputMode: InputMode,
    controller: InputModeController,
    focusRequester: NativeFocusRequester,
    onFullscreenText: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.then(if (showExpandButton) Modifier.fillMaxHeight() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 全屏输入按钮
        if (showExpandButton) {
            CircleActionIcon(iconResId = R.drawable.ic_expend_outlined, onClick = onFullscreenText)
        }

        // 语音/文本切换按钮
        ActionIcon(
            iconResId = if (inputMode.isVoice) {
                R.drawable.ic_keyboard_outlined
            } else {
                R.drawable.ic_voice_outlined
            }
        ) {
            if (inputMode.isVoice) {
                controller.switchMode(InputMode.TEXT)
                // 自动弹出键盘
                scope.launch {
                    delay(50)
                    focusRequester.requestFocus()
                }
            } else {
                controller.switchMode(InputMode.VOICE)
            }
        }
    }
}

@Composable
private fun RowScope.InputBox(
    text: String,
    inputMode: InputMode,
    focusRequester: NativeFocusRequester,
    onTextChange: (String) -> Unit,
    onLineCountChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .defaultMinSize(minHeight = 40.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (inputMode.isVoice) {
            // 语音模式：显示“按住说话”
            VoiceRecordButton()
        } else {
            // 其他所有模式：都显示输入框
            EmojiTextField(
                value = text,
                focusRequester = focusRequester,
                onValueChange = onTextChange,
                onLineCountChange = onLineCountChange
            )
        }
    }
}

@Composable
private fun EmojiButton(inputMode: InputMode, controller: InputModeController) {
    ActionIcon(
        iconResId = if (inputMode.isEmoji) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_emoji_outlined
        }
    ) {
        val mode = if (inputMode.isEmoji) InputMode.TEXT else InputMode.EMOJI
        controller.switchMode(mode)
    }
}

@Composable
private fun SendOrMoreButton(
    text: String,
    onSend: () -> Unit,
    inputMode: InputMode,
    controller: InputModeController
) {
    AnimatedContent(targetState = text.isNotEmpty(), label = "SendBtn") { isNotEmpty ->
        if (isNotEmpty) {
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                WeButton("发送", size = ButtonSize.SMALL, onClick = onSend)
            }
        } else {
            ActionIcon(iconResId = R.drawable.ic_plus_circle_outlined) {
                val mode =
                    if (inputMode.isMore) InputMode.TEXT else InputMode.MORE
                controller.switchMode(mode)
            }
        }
    }
}