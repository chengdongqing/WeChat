package top.chengdongqing.wechat.feature.chat.ui.session.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.EmojiTextField
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.feature.chat.domain.model.InputMode
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.ActionIcon
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionUiState
import top.chengdongqing.wechat.feature.chat.ui.session.ChatSessionViewModel
import top.chengdongqing.wechat.feature.chat.ui.session.CircleActionIcon
import top.chengdongqing.wechat.feature.chat.ui.session.input.music.MusicOverlay
import top.chengdongqing.wechat.feature.chat.ui.session.input.panel.InputPanelHolder
import top.chengdongqing.wechat.feature.chat.ui.session.input.text.InputOverlay
import top.chengdongqing.wechat.feature.chat.ui.session.input.text.SpeechInputButton
import top.chengdongqing.wechat.feature.chat.ui.session.input.voice.VoiceRecordButton
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.feature.chat.ui.session.util.ScrollToDismissEffect
import kotlin.time.Duration.Companion.milliseconds

/**
 * 聊天输入栏
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputBar(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    listState: LazyListState,
    onLaunchCall: (type: CallType) -> Unit,
    onStartLive: () -> Unit,
    onShareLiveLocation: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val focusRequester = remember { NativeFocusRequester() }
    val controller = rememberInputBarController(focusRequester, uiState.isSendButtonOn)
    val state by controller.state.collectAsStateWithLifecycle()
    val pendingQuote by viewModel.pendingQuote.collectAsStateWithLifecycle()
    val actions = rememberInputBarActions(
        controller = controller,
        onSendMessage = viewModel::sendMessage,
        onLaunchCall = onLaunchCall,
        onStartLive = onStartLive,
        onShareLiveLocation = onShareLiveLocation,
        onOpenFavorites = onOpenFavorites
    )
    var showMentionPicker by remember { mutableStateOf(false) }
    val inputActions = remember(actions, state.inputText, uiState.mentionMembers) {
        actions.copy(
            onTextChange = { newText ->
                val oldText = controller.state.value.inputText
                controller.updateText(newText)
                if (
                    uiState.mentionMembers.isNotEmpty() &&
                    newText.length > oldText.length &&
                    newText.endsWith("@")
                ) {
                    showMentionPicker = true
                }
            }
        )
    }

    ScrollToDismissEffect(
        listState = listState,
        isPanelMode = state.inputMode.isPanelMode,
        onDismiss = controller::dismissAll
    )

    /**
     * 恢复草稿消息
     */
    LaunchedEffect(uiState.draftMessage) {
        uiState.draftMessage?.let {
            controller.updateText(it)
            delay(500.milliseconds)
            // 自动弹出键盘
            focusRequester.requestFocus()
        }
    }

    /**
     * 重新编辑消息
     */
    LaunchedEffect(Unit) {
        viewModel.uiEvent
            .filterIsInstance<MessageUiEvent.ReeditMessage>()
            .collect { event ->
                controller.updateText(controller.state.value.inputText + event.text)
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 退出页面时收起键盘
            controller.dismissAll()
            // 保存草稿消息
            viewModel.saveDraftMessage(state.inputText)
        }
    }

    Column(
        modifier = Modifier
            .background(ChatTheme.colorScheme.bottomBarBackground)
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
    ) {
        pendingQuote?.let { quote ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (quote.senderId == uiState.myId) "我" else uiState.title,
                        color = WeTheme.colorScheme.textSecondary
                    )
                    Text(
                        text = quote.preview,
                        color = WeTheme.colorScheme.textSecondary,
                        maxLines = 2
                    )
                }
                ActionIcon(
                    icon = R.drawable.ic_close_outlined,
                    onClick = viewModel::cancelQuote
                )
            }
        }
        InputMainSection(
            state = state,
            actions = inputActions,
            focusRequester = controller.focusRequester
        )
        InputPanelHolder(
            inputMode = state.inputMode,
            actions = inputActions,
            recentEmojis = state.recentEmojis
        )
    }

    InputOverlay(state, inputActions)
    MusicOverlay(state, actions)

    if (showMentionPicker) {
        AlertDialog(
            onDismissRequest = { showMentionPicker = false },
            title = { Text("选择提醒的人") },
            text = {
                LazyColumn {
                    item {
                        MentionPickerItem("所有人", null) {
                            controller.insertMention("所有人")
                            showMentionPicker = false
                        }
                    }
                    items(uiState.mentionMembers, key = { it.id }) { member ->
                        MentionPickerItem(member.name, member.avatarPath) {
                            controller.insertMention(member.name)
                            showMentionPicker = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMentionPicker = false }) { Text("取消") }
            }
        )
    }
}

private fun InputBarController.insertMention(name: String) {
    val current = state.value.inputText
    val atIndex = current.lastIndexOf('@')
    updateText(
        if (atIndex >= 0) current.substring(0, atIndex) + "@$name "
        else "$current@$name "
    )
    focusRequester.requestFocus()
}

@Composable
private fun MentionPickerItem(name: String, avatarPath: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarPath,
            error = painterResource(R.drawable.img_avatar_placeholder),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(name, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun InputMainSection(
    state: InputBarState,
    actions: InputBarActions,
    focusRequester: NativeFocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        VoiceModeToggle(state, actions)
        InputFieldArea(state, actions, focusRequester, modifier = Modifier.weight(1f))
        EmojiToggle(state.inputMode, actions, focusRequester)
        SendOrMoreToggle(state, actions, focusRequester)
    }
}

@Composable
private fun VoiceModeToggle(state: InputBarState, actions: InputBarActions) {
    Column(
        modifier = if (state.shouldShowExpandButton) Modifier.fillMaxHeight() else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (state.shouldShowExpandButton) {
            CircleActionIcon(
                icon = R.drawable.ic_expend_outlined,
                onClick = actions.onToggleExpand
            )
        }
        ActionIcon(
            icon = if (state.inputMode.isVoice) {
                R.drawable.ic_keyboard_outlined
            } else {
                R.drawable.ic_voice_circle_outlined
            }
        ) {
            if (state.inputMode.isVoice) actions.onSwitchToText()
            else actions.onSwitchToVoice()
        }
    }
}

@Composable
private fun InputFieldArea(
    state: InputBarState,
    actions: InputBarActions,
    focusRequester: NativeFocusRequester,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .defaultMinSize(minHeight = 40.dp)
            .background(ChatTheme.colorScheme.textField, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (state.inputMode.isVoice) {
            VoiceRecordButton(
                onVoiceSend = { localPath, duration ->
                    val content = MessageContent.Voice(localPath, duration)
                    actions.onSendMessage(content)
                },
                onConvertToText = { _, _ -> }
            )
        } else {
            key(state.isSendButtonOn) {
                EmojiTextField(
                    value = state.inputText,
                    focusRequester = focusRequester,
                    onValueChange = actions.onTextChange,
                    onLineCountChange = actions.onLineCountChange,
                    imeAction = if (state.isSendButtonOn) {
                        ImeAction.Default
                    } else {
                        ImeAction.Send
                    },
                    onImeAction = {
                        if (state.inputText.isNotBlank()) {
                            actions.onSendText()
                        }
                    },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            SpeechInputButton(
                onResult = actions.onSpeechResult,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun EmojiToggle(
    inputMode: InputMode,
    actions: InputBarActions,
    focusRequester: NativeFocusRequester
) {
    ActionIcon(
        icon = if (inputMode.isEmoji) {
            R.drawable.ic_keyboard_outlined
        } else {
            R.drawable.ic_emoji_outlined
        }
    ) {
        if (!inputMode.isEmoji) {
            actions.onSwitchMode(InputMode.Emoji)
        } else {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun SendOrMoreToggle(
    state: InputBarState,
    actions: InputBarActions,
    focusRequester: NativeFocusRequester
) {
    AnimatedContent(
        targetState = state.shouldShowSendButton,
        label = "SendButtonAnimation"
    ) { shouldShow ->
        if (shouldShow) {
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                WeButton(
                    text = stringResource(R.string.action_send),
                    size = ButtonSize.Small,
                    onClick = actions.onSendText
                )
            }
        } else {
            ActionIcon(icon = R.drawable.ic_plus_circle_outlined) {
                if (!state.inputMode.isMore) {
                    actions.onSwitchMode(InputMode.More)
                } else {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}
