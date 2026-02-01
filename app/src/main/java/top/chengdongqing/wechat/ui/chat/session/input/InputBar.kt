package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.media.rememberSoundTipPlayer
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.ActionIcon
import top.chengdongqing.wechat.ui.chat.session.CircleActionIcon
import top.chengdongqing.wechat.ui.chat.session.ScrollToDismissEffect
import top.chengdongqing.wechat.ui.chat.session.input.handler.ActionHandler
import top.chengdongqing.wechat.ui.chat.session.input.handler.rememberActionHandler
import top.chengdongqing.wechat.ui.chat.session.input.handler.rememberLocationHandler
import top.chengdongqing.wechat.ui.chat.session.input.handler.rememberMediaHandler
import top.chengdongqing.wechat.ui.chat.session.input.panel.InputPanelHolder
import top.chengdongqing.wechat.ui.chat.session.input.text.InputOverlay
import top.chengdongqing.wechat.ui.chat.session.input.voice.VoiceRecordButton
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.dialog.rememberDialogState
import top.chengdongqing.wechat.ui.components.emojitextfield.EmojiTextField
import top.chengdongqing.wechat.ui.components.emojitextfield.NativeFocusRequester

/**
 * 聊天输入栏
 *
 * 功能：
 * - 文本输入
 * - 语音录制
 * - 表情面板
 * - 更多功能面板
 * - 全屏输入等
 *
 * @param listState 消息列表状态
 * @param onSendMessage 发送消息回调
 */
@Composable
fun InputBar(
    listState: LazyListState,
    isSending: Boolean,
    onSendMessage: (MessageContent, onSent: (() -> Unit)?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { NativeFocusRequester() }

    // 控制器
    val controller = rememberInputBarController(focusRequester)
    val state by controller.state.collectAsStateWithLifecycle()
    val inputMode = state.inputMode

    // 创建handler
    val mediaHandler = rememberMediaHandler(
        context = context,
        scope = scope,
        onSendMessage = onSendMessage,
        onModeSwitch = { controller.dismissAll() }
    )
    val locationHandler = rememberLocationHandler(onSendMessage)

    // 更多操作处理器
    val actionHandler = rememberActionHandler(
        context = context,
        mediaHandler = mediaHandler,
        locationHandler = locationHandler,
        onSendMessage = onSendMessage
    )

    // 对话框状态
    val dialogState = rememberDialogState()
    val soundPlayer = rememberSoundTipPlayer()

    // 自动收起面板/键盘（当用户滚动消息列表时）
    ScrollToDismissEffect(
        listState = listState,
        isSending = isSending,
        isPanelMode = inputMode.isPanelMode,
        onDismiss = controller::dismissAll
    )

    // 发送文本消息
    val sendTextMessage = {
        if (state.inputText.isNotBlank()) {
            onSendMessage(MessageContent.Text(state.inputText), null)
            controller.clearInput()
        } else {
            dialogState.show("提示", "不能发送空白消息", onCancel = null)
        }
    }

    Column(
        modifier = Modifier
            .background(Color(0xFFF7F7F7))
            .navigationBarsPadding()
    ) {
        // 主输入区域
        InputMainSection(
            state = state,
            controller = controller,
            soundPlayer = soundPlayer,
            onSendText = sendTextMessage,
            onSendMessage = onSendMessage
        )

        // 面板区域（表情、更多功能）
        InputPanelSection(
            inputMode = inputMode,
            controller = controller,
            actionHandler = actionHandler,
            onSendMessage = onSendMessage
        )
    }

    // 全屏输入框
    InputOverlay(
        visible = state.isExpanded,
        inputText = state.inputText,
        onTextChange = controller::updateText,
        onClose = controller::toggleExpand
    )
}

/**
 * 主输入区域
 */
@Composable
private fun InputMainSection(
    state: InputBarState,
    controller: InputBarController,
    soundPlayer: SoundTipPlayer,
    onSendText: () -> Unit,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 左侧：语音/文字切换按钮
        VoiceModeToggle(
            state = state,
            controller = controller,
            onExpand = controller::toggleExpand
        )

        // 中间：输入框区域
        InputFieldArea(
            state = state,
            controller = controller,
            soundPlayer = soundPlayer,
            onSendMessage = onSendMessage,
            modifier = Modifier.weight(1f)
        )

        // 右侧：表情按钮
        EmojiToggle(
            inputMode = state.inputMode,
            controller = controller
        )

        // 右侧：发送/更多按钮
        SendOrMoreToggle(
            state = state,
            controller = controller,
            onSendText = onSendText
        )
    }
}

/**
 * 面板区域
 */
@Composable
private fun InputPanelSection(
    inputMode: InputMode,
    controller: InputBarController,
    actionHandler: ActionHandler,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit
) {
    InputPanelHolder(
        inputMode = inputMode,
        onEmojiSelect = { emoji ->
            controller.insertEmoji(emoji.description)
        },
        onStickerSelect = { sticker ->
            onSendMessage(sticker, null)
        },
        onBackspace = controller::handleEmojiBackspace,
        onMoreAction = actionHandler::handleAction
    )
}

/**
 * 语音/文字模式切换
 */
@Composable
private fun VoiceModeToggle(
    state: InputBarState,
    controller: InputBarController,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.then(
            if (state.shouldShowExpandButton) {
                Modifier.fillMaxHeight()
            } else {
                Modifier
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 全屏输入按钮
        if (state.shouldShowExpandButton) {
            CircleActionIcon(
                iconResId = R.drawable.ic_expend_outlined,
                onClick = onExpand
            )
        }

        // 语音/文本切换按钮
        ActionIcon(
            iconResId = if (state.inputMode.isVoice) {
                R.drawable.ic_keyboard_outlined
            } else {
                R.drawable.ic_voice_circle_outlined
            }
        ) {
            if (state.inputMode.isVoice) {
                controller.switchToTextMode()

                // 自动弹出键盘
                scope.launch {
                    delay(50)
                    controller.focusRequester.requestFocus()
                }
            } else {
                controller.switchMode(InputMode.Voice)
            }
        }
    }
}

/**
 * 输入框区域
 */
@Composable
private fun InputFieldArea(
    state: InputBarState,
    controller: InputBarController,
    soundPlayer: SoundTipPlayer,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .defaultMinSize(minHeight = 40.dp)
            .background(Color.White, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (state.inputMode.isVoice) {
            // 语音录制按钮
            VoiceRecordButton(
                onVoiceSend = { uri, duration ->
                    val content = MessageContent.Voice(uri, duration)
                    onSendMessage(content) {
                        soundPlayer.play(R.raw.after_upload_voice)
                    }
                },
                onConvertToText = { _, _ -> }
            )
        } else {
            // 文本输入框
            EmojiTextField(
                value = state.inputText,
                focusRequester = controller.focusRequester,
                onValueChange = controller::updateText,
                onLineCountChange = controller::updateLineCount
            )
        }
    }
}

/**
 * 表情切换按钮
 */
@Composable
private fun EmojiToggle(
    inputMode: InputMode,
    controller: InputBarController,
    modifier: Modifier = Modifier
) {
    ActionIcon(
        iconResId = if (inputMode.isEmoji) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_emoji_outlined
        },
        modifier = modifier
    ) {
        val mode = if (inputMode.isEmoji) {
            InputMode.Text
        } else {
            InputMode.Emoji
        }
        controller.switchMode(mode)
    }
}

/**
 * 发送/更多切换按钮
 */
@Composable
private fun SendOrMoreToggle(
    state: InputBarState,
    controller: InputBarController,
    onSendText: () -> Unit,
) {
    AnimatedContent(
        targetState = state.shouldShowSendButton,
        label = "SendButtonAnimation"
    ) { shouldShow ->
        if (shouldShow) {
            Box(
                modifier = Modifier.height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                WeButton(
                    text = "发送",
                    size = ButtonSize.Small,
                    onClick = onSendText
                )
            }
        } else {
            ActionIcon(
                iconResId = R.drawable.ic_plus_circle_outlined
            ) {
                val mode = if (state.inputMode.isMore) {
                    InputMode.Text
                } else {
                    InputMode.More
                }
                controller.switchMode(mode)
            }
        }
    }
}