package top.chengdongqing.wechat.ui.chat.session.input

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester
import top.chengdongqing.wechat.ui.utils.weClickable

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

    Column(
        modifier = Modifier
            .background(Color(0xFFF7F7F7))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 语音/文字切换
            VoiceButton(inputMode, controller, focusRequester)
            // 输入框区域
            InputBox(
                text,
                onTextChange = { newText ->
                    if (newText.length < text.length) {
                        text.handleBackspace(focusRequester.selectionStart) { newString, _ ->
                            onTextChange(newString)
                        }
                    } else {
                        onTextChange(newText)
                    }
                },
                inputMode,
                focusRequester
            )
            // 表情按钮
            EmojiButton(inputMode, controller)
            // 发送/更多按钮
            SendOrMoreButton(text, onSend, inputMode, controller)
        }

        InputPanelHolder(
            inputMode,
            onEmojiSelect = {
                val insertText = "[${it.description}]"
                val cursorIndex = focusRequester.selectionStart
                val newText = StringBuilder(text)
                    .insert(cursorIndex, insertText)
                    .toString()
                onTextChange(newText)

                // 计算新的光标位置
                val newCursorIndex = cursorIndex + insertText.length
                scope.launch {
                    delay(16)
                    focusRequester.setSelection(newCursorIndex)
                }
            },
            onStickerSelect = {

            },
            onBackspace = {
                text.handleBackspace(focusRequester.selectionStart) { newString, newPos ->
                    onTextChange(newString)

                    // 同步光标
                    focusRequester.post {
                        focusRequester.setSelection(newPos)
                    }
                }
            }
        )
    }
}

@Composable
private fun VoiceButton(
    inputMode: InputMode,
    controller: InputModeController,
    focusRequester: NativeFocusRequester
) {
    val scope = rememberCoroutineScope()

    ActionIcon(
        iconResId = if (inputMode.isVoice) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_voice_outlined
        }
    ) {
        if (inputMode.isVoice) {
            controller.switchMode(InputMode.Companion.TEXT)
            // 自动弹出键盘
            scope.launch {
                delay(50)
                focusRequester.requestFocus()
            }
        } else {
            controller.switchMode(InputMode.Companion.VOICE)
        }
    }
}

@Composable
private fun RowScope.InputBox(
    text: String,
    onTextChange: (String) -> Unit,
    inputMode: InputMode,
    focusRequester: NativeFocusRequester
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
                onValueChange = onTextChange,
                focusRequester = focusRequester
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
            R.drawable.ic_sticker_outlined
        }
    ) {
        val mode = if (inputMode.isEmoji) InputMode.Companion.TEXT else InputMode.Companion.EMOJI
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
                    if (inputMode.isMore) InputMode.Companion.TEXT else InputMode.Companion.MORE
                controller.switchMode(mode)
            }
        }
    }
}

@Composable
private fun ActionIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    description: String? = null,
    tint: Color = WeChatTheme.colorScheme.textPrimary,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .weClickable(onClick = { onClick?.invoke() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = description,
            modifier = Modifier.size(30.dp),
            tint = tint
        )
    }
}

/**
 * 处理文本删除
 */
private fun String.handleBackspace(
    selectionStart: Int,
    onChange: (newText: String, newCursorPos: Int) -> Unit
) {
    if (selectionStart <= 0) return

    val textBefore = substring(0, selectionStart)
    val textAfter = substring(selectionStart)

    // 匹配光标左侧紧邻的 "[xxx]"
    // 正则：以 [ 开头，中间包含非括号字符，以 ] 结尾，且必须紧贴末尾($)
    val emojiRegex = Regex("\\[[^\\[\\]]+]$")
    val match = emojiRegex.find(textBefore)

    if (match != null) {
        // A：光标前是表情块，整体删除
        val newText = textBefore.removeRange(match.range) + textAfter
        val newCursorPos = match.range.first
        onChange(newText, newCursorPos)
    } else {
        // B：普通文本，只删一个字符
        val newText = textBefore.dropLast(1) + textAfter
        val newCursorPos = selectionStart - 1
        onChange(newText, newCursorPos)
    }
}