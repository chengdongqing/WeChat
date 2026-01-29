package top.chengdongqing.wechat.ui.chat.session.input

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.core.utils.createMediaUri
import top.chengdongqing.wechat.core.utils.prepareMediaResource
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.CallStatus
import top.chengdongqing.wechat.data.model.CallType
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.chat.session.ActionIcon
import top.chengdongqing.wechat.ui.chat.session.CircleActionIcon
import top.chengdongqing.wechat.ui.chat.session.ScrollToDismissEffect
import top.chengdongqing.wechat.ui.chat.session.input.panels.MoreAction
import top.chengdongqing.wechat.ui.chat.session.input.text.InputOverlay
import top.chengdongqing.wechat.ui.chat.session.input.voice.VoiceRecordButton
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.components.camera.rememberCameraLauncher
import top.chengdongqing.wechat.ui.components.dialog.rememberDialogState
import top.chengdongqing.wechat.ui.components.emojitextfield.EmojiTextField
import top.chengdongqing.wechat.ui.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.ui.components.location.picker.rememberPickLocationLauncher
import top.chengdongqing.wechat.ui.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.rememberToggleState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun InputBar(
    listState: LazyListState,
    isSending: Boolean,
    onSend: (content: MessageContent, onSent: (() -> Unit)?) -> Unit
) {
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

    val onSend = { content: MessageContent ->
        onSend(content, null)
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

    val launchMediaPicker = rememberPickMediasLauncher { items ->
        // 切换回文本模式
        controller.switchMode(showKeyboard = false)

        // 将数据转换为统一的消息内容格式
        val contents = items.map { item ->
            if (item.isImage) {
                MessageContent.Image(
                    uri = item.uri,
                    mimeType = item.mimeType,
                    filename = item.filename,
                    width = item.width,
                    height = item.height
                )
            } else {
                MessageContent.Video(
                    uri = item.uri,
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

    val context = LocalContext.current
    val launchCamera = rememberCameraLauncher { mediaUri, mediaType ->
        scope.launch(Dispatchers.IO) {
            val res = prepareMediaResource(context, mediaUri) ?: return@launch

            withContext(Dispatchers.Main) {
                val content = if (mediaType.isImage) {
                    MessageContent.Image(
                        uri = mediaUri,
                        mimeType = res.mimeType,
                        filename = res.filename,
                        width = res.width,
                        height = res.height
                    )
                } else {
                    MessageContent.Video(
                        uri = mediaUri,
                        mimeType = res.mimeType,
                        filename = res.filename,
                        width = res.width,
                        height = res.height,
                        duration = res.duration
                    )
                }
                onSend(content)
            }
        }
    }

    val actionSheet = rememberActionSheetState()

    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    val takeMediaOptions = remember {
        listOf(
            ActionSheetItem("拍摄照片"),
            ActionSheetItem("拍摄视频")
        )
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedUri?.let { uri ->
                scope.launch(Dispatchers.IO) {
                    val res = prepareMediaResource(context, uri) ?: return@launch

                    withContext(Dispatchers.Main) {
                        val content = MessageContent.Image(
                            uri = uri,
                            mimeType = res.mimeType,
                            filename = res.filename,
                            width = res.width,
                            height = res.height
                        )
                        onSend(content)
                    }
                }
            }
        }
    }
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            capturedUri?.let { uri ->
                scope.launch(Dispatchers.IO) {
                    val res = prepareMediaResource(context, uri) ?: return@launch

                    withContext(Dispatchers.Main) {
                        val content = MessageContent.Video(
                            uri = uri,
                            mimeType = res.mimeType,
                            filename = res.filename,
                            width = res.width,
                            height = res.height,
                            duration = res.duration
                        )
                        onSend(content)
                    }
                }
            }
        }
    }

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

    val locationOptions = remember {
        listOf(
            ActionSheetItem("发送位置"),
            ActionSheetItem("共享实时位置")
        )
    }
    val launchLocationPicker = rememberPickLocationLauncher { location ->
        val latLng = location.latLng
        val content = MessageContent.Location(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            address = location.address ?: "",
            poiName = location.poiName,
            snapshotUri = location.snapshotUri
        )

        onSend(content)
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
                onLineCountChange = { lineCount = it },
                onVoiceSend = { uri, duration ->
                    val content = MessageContent.Voice(uri, duration)
                    onSend(content) {
                        SoundTipPlayer.play(R.raw.after_upload_voice) // 播放提示音
                    }
                }
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
        ) { actionId, isLongClick ->
            when (actionId) {
                MoreAction.ALBUM -> launchMediaPicker(VisualMediaType.IMAGE_AND_VIDEO, 9)
                MoreAction.CAMERA -> {
                    if (isLongClick) {
                        actionSheet.show(takeMediaOptions, "调用系统相机") { index ->
                            when (index) {
                                0 -> {
                                    val uri = context.createMediaUri(false)
                                    capturedUri = uri
                                    takePictureLauncher.launch(uri)
                                }

                                1 -> {
                                    val uri = context.createMediaUri(true)
                                    capturedUri = uri
                                    captureVideoLauncher.launch(uri)
                                }
                            }
                        }
                    } else {
                        launchCamera(VisualMediaType.IMAGE_AND_VIDEO)
                    }
                }

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

                MoreAction.LOCATION -> {
                    actionSheet.show(locationOptions) { index ->
                        when (index) {
                            0 -> launchLocationPicker()
                        }
                    }
                }

                MoreAction.FILE -> {
                    val content = MessageContent.File(
                        fileName = "extra_data.b",
                        fileSize = (6.9 * 1024 * 1024).toLong(),
                        fileType = "file",
                        fileUrl = ""
                    )
                    onSend(content)
                }

                MoreAction.CARD -> {
                    val content = MessageContent.UserCard(
                        userId = randomUUID(),
                        name = "文件传输助手",
                        avatar = ""
                    )
                    onSend(content)
                }

                MoreAction.FAVORITE -> {}
                MoreAction.VOICE -> {}
                else -> {}
            }
        }
    }

    // 全屏输入框
    InputOverlay(
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
                R.drawable.ic_voice_circle_outlined
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
    onLineCountChange: (Int) -> Unit,
    onVoiceSend: (uri: Uri, duration: Long) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .defaultMinSize(minHeight = 40.dp)
            .background(Color.White, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (inputMode.isVoice) {
            // 语音模式：显示“按住说话”
            VoiceRecordButton(onVoiceSend, onConvertToText = { _, _ -> })
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