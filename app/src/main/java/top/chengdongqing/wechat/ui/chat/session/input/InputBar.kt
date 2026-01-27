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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.CallStatus
import top.chengdongqing.wechat.data.model.CallType
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.data.model.isImage
import top.chengdongqing.wechat.ui.chat.session.ActionIcon
import top.chengdongqing.wechat.ui.chat.session.CircleActionIcon
import top.chengdongqing.wechat.ui.chat.session.ScrollToDismissEffect
import top.chengdongqing.wechat.ui.chat.session.input.panels.MoreAction
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.dialog.rememberDialogState
import top.chengdongqing.wechat.ui.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester
import top.chengdongqing.wechat.ui.utils.rememberToggleState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun InputBar(listState: LazyListState, isSending: Boolean, onSend: (MessageContent) -> Unit) {
    val focusRequester = remember { NativeFocusRequester() }
    val controller = rememberInputModeController(focusRequester)
    val inputMode by controller.inputMode
    val scope = rememberCoroutineScope()

    // 显示键盘/展开面板时，如果用户主动滚动消息列表，则收起底部
    ScrollToDismissEffect(listState, isSending, inputMode.isPanelMode) {
        controller.switchMode(showKeyboard = false)
    }

    // 当前输入的内容
    var inputText by remember { mutableStateOf("") }
    // 当前输入框行数
    var lineCount by remember { mutableIntStateOf(1) }
    // 是否启用全屏输入
    val (isExpanded, toggleExpand) = rememberToggleState(
        defaultValue = false,
        reverseValue = true
    )
    val onTextChange = { newText: String ->
        inputText = newText
    }

    val currentText = rememberUpdatedState(inputText)
    val inputHandler = remember {
        InputHandler(currentText, focusRequester, scope, onTextChange)
    }

    val dialog = rememberDialogState()
    val sendTextMessage = {
        if (inputText.isNotBlank()) {
            onSend(MessageContent.Text(inputText))
            onTextChange("")
        } else {
            dialog.show("提示", "不能发送空白消息", onCancel = null)
        }
    }

    val pickMedia = rememberPickMediasLauncher { items ->
        // 将数据转换为统一的消息内容格式
        val contents = items.map { item ->
            if (item.isImage()) {
                MessageContent.Image(
                    url = item.uri.toString(),
                    mimeType = item.mimeType,
                    filename = item.filename,
                    width = item.width,
                    height = item.height
                )
            } else {
                MessageContent.Video(
                    videoUrl = item.uri.toString(),
                    mimeType = item.mimeType,
                    filename = item.filename,
                    width = item.width,
                    height = item.height,
                    duration = item.duration
                )
            }
        }

        // 批量发送
        scope.launch {
            contents.forEach { content ->
                onSend(content)
                // 留一点缓冲时间
                delay(50)
            }
        }
    }

    val actionSheet = rememberActionSheetState()
    val callOptions = remember {
        listOf(
            ActionSheetItem("视频通话", icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_video_call_filled),
                    contentDescription = null,
                    tint = WeChatTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }),
            ActionSheetItem("语音通话", icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_voice_call_filled),
                    contentDescription = null,
                    tint = WeChatTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            })
        )
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
                onExpand = { toggleExpand() }
            )
            // 输入框区域
            InputBox(
                text = inputText,
                inputMode = inputMode,
                focusRequester = focusRequester,
                onTextChange = onTextChange,
                onLineCountChange = { lineCount = it }
            )
            // 表情按钮
            EmojiButton(inputMode, controller)
            // 发送/更多按钮
            SendOrMoreButton(inputText, inputMode, controller, sendTextMessage)
        }

        InputPanelHolder(
            inputMode,
            onEmojiSelect = { inputHandler.insertEmoji(it.description) },
            onStickerSelect = onSend,
            onBackspace = inputHandler::handleEmojiBackspace
        ) { action ->
            when (action) {
                MoreAction.ALBUM -> pickMedia(VisualMediaType.IMAGE_AND_VIDEO, 9)
                MoreAction.CAMERA -> {}
                MoreAction.VIDEO_CALL -> {
                    actionSheet.show(callOptions) { index ->
                        val content = MessageContent.Call(
                            type = if (index == 0) CallType.VIDEO else CallType.VOICE,
                            status = CallStatus.CONNECTED,
                            duration = (3.minutes + 26.seconds).toLong(DurationUnit.MILLISECONDS)
                        )
                        onSend(content)
                    }
                }

                MoreAction.LOCATION -> {}
                MoreAction.FAVORITE -> {}
                MoreAction.VOICE -> {}
                MoreAction.CARD -> {}
                MoreAction.FILE -> {}
                else -> {}
            }
        }
    }

    // 全屏输入框
    FullScreenInputPopup(
        visible = isExpanded.value,
        text = inputText,
        onTextChange = onTextChange,
        onClose = toggleExpand::invoke
    )
}

@Composable
private fun VoiceButton(
    showExpandButton: Boolean = false,
    inputMode: InputMode,
    controller: InputModeController,
    focusRequester: NativeFocusRequester,
    onExpand: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.then(if (showExpandButton) Modifier.fillMaxHeight() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 全屏输入按钮
        if (showExpandButton) {
            CircleActionIcon(iconResId = R.drawable.ic_expend_outlined, onClick = onExpand)
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
                controller.switchMode()
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
    inputMode: InputMode,
    controller: InputModeController,
    onSend: () -> Unit
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