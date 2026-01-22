package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.UpdatedEffect
import top.chengdongqing.wechat.core.utils.weClickable
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val controller = rememberInputModeController(focusRequester)
    val inputMode by controller.inputMode

    // 切换到文本模式后自动弹出键盘
    FocusRequestEffect(focusRequester, inputMode.isText)

    /**
     * 处理文本删除
     */
    fun handleBackspace() {
        if (text.isNotEmpty()) {
            // 匹配末尾是否是 "[xxx]" 这种格式
            val lastBracketIndex = text.lastIndexOf('[')
            val lastChar = text.last()

            if (lastChar == ']' && lastBracketIndex != -1) {
                // 进一步确认括号内是否有内容，或者是否符合表情格式
                onTextChange(text.take(lastBracketIndex))
            } else {
                // 普通文本，只删除最后一个字符
                onTextChange(text.dropLast(1))
            }
        }
    }

    Column(
        modifier = Modifier
            .background(Color(0xFFF7F7F7))
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 语音/文字切换
            VoiceButton(inputMode, controller)
            // 输入框区域
            InputBox(
                text,
                onTextChange = { newText ->
                    if (newText.length < text.length) {
                        handleBackspace()
                    } else {
                        onTextChange(newText)
                    }
                },
                inputMode,
                focusRequester,
            )
            // 表情按钮
            EmojiButton(inputMode, controller)
            // 发送/更多按钮
            SendOrMoreButton(text, onSend, inputMode, controller)
        }

        if (inputMode.isPanelMode) {
            ExpandablePanel(
                inputMode,
                onEmojiSelect = {
                    onTextChange(text + "[${it.description}]")
                },
                onStickerSelect = {

                },
                onBackspace = {
                    handleBackspace()
                }
            )
        }
    }
}

@Composable
private fun VoiceButton(
    inputMode: ChatInputMode,
    controller: InputModeController
) {
    ActionIcon(
        iconResId = if (inputMode.isVoice) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_voice_outlined
        }
    ) {
        if (inputMode.isVoice) {
            controller.switchMode(ChatInputMode.TEXT)
        } else {
            controller.switchMode(ChatInputMode.VOICE)
        }
    }
}

@Composable
private fun RowScope.InputBox(
    text: String,
    onTextChange: (String) -> Unit,
    inputMode: ChatInputMode,
    focusRequester: FocusRequester,
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
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }
}

@Composable
private fun VoiceRecordButton() {
    Text(
        "按住 说话",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EmojiButton(inputMode: ChatInputMode, controller: InputModeController) {
    ActionIcon(
        iconResId = if (inputMode.isEmoji) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_sticker_outlined
        }
    ) {
        val mode = if (inputMode.isEmoji) ChatInputMode.TEXT else ChatInputMode.EMOJI
        controller.switchMode(mode)
    }
}

@Composable
private fun SendOrMoreButton(
    text: String,
    onSend: () -> Unit,
    inputMode: ChatInputMode,
    controller: InputModeController
) {
    AnimatedContent(targetState = text.isNotEmpty(), label = "SendBtn") { isNotEmpty ->
        if (isNotEmpty) {
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                WeButton("发送", size = ButtonSize.SMALL, onClick = onSend)
            }
        } else {
            ActionIcon(iconResId = R.drawable.ic_plus_circle_outlined) {
                val mode = if (inputMode.isMore) ChatInputMode.TEXT else ChatInputMode.MORE
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

@Composable
private fun FocusRequestEffect(
    focusRequester: FocusRequester,
    trigger: Boolean
) {
    UpdatedEffect(trigger) {
        if (trigger) {
            focusRequester.requestFocus()
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ChatBottomBar("", {}, {})
}