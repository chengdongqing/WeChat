package top.chengdongqing.wechat.features.chat.ui.session.input

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.EmojiTextField
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.InputMode
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.theme.ChatTheme
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionUiState
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionViewModel
import top.chengdongqing.wechat.features.chat.ui.session.components.ActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.components.CircleActionIcon
import top.chengdongqing.wechat.features.chat.ui.session.input.music.MusicOverlay
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.InputPanelHolder
import top.chengdongqing.wechat.features.chat.ui.session.input.text.InputOverlay
import top.chengdongqing.wechat.features.chat.ui.session.input.text.SpeechInputButton
import top.chengdongqing.wechat.features.chat.ui.session.input.voice.VoiceRecordButton
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent
import top.chengdongqing.wechat.features.chat.ui.session.util.ScrollToDismissEffect

/**
 * 聊天输入栏
 */
@Composable
fun InputBar(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    listState: LazyListState,
    onLaunchCall: (type: CallType) -> Unit
) {
    val focusRequester = remember { NativeFocusRequester() }
    val controller = rememberInputBarController(focusRequester, uiState.isSendButtonOn)
    val state by controller.state.collectAsStateWithLifecycle()
    val actions = rememberInputBarActions(
        controller = controller,
        onSendMessage = viewModel::sendMessage,
        onLaunchCall = onLaunchCall
    )

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
            delay(500)
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
            .navigationBarsPadding()
    ) {
        InputMainSection(
            state = state,
            actions = actions,
            focusRequester = controller.focusRequester
        )
        InputPanelHolder(
            inputMode = state.inputMode,
            actions = actions,
            recentEmojis = state.recentEmojis
        )
    }

    InputOverlay(state, actions)
    MusicOverlay(state, actions)
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
                WeButton(text = "发送", size = ButtonSize.Small, onClick = actions.onSendText)
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