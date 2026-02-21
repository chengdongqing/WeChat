package top.chengdongqing.wechat.features.chat.ui.session.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberActionHandler
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberFileHandler
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberFileLauncher
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberLocationHandler
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberLocationLauncher
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberMediaHandler
import top.chengdongqing.wechat.features.chat.ui.session.input.handler.rememberMediaLaunchers

/**
 * 输入栏 Actions 唯一组装入口
 *
 * 将所有 handler / launcher / controller 编排成 [InputBarActions]，
 * 调用方（InputBar）只需拿到这一个对象，不再感知内部依赖。
 */
@Composable
fun rememberInputBarActions(
    controller: InputBarController,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    onLaunchCall: (CallType) -> Unit,
    soundPlayer: SoundTipPlayer
): InputBarActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by controller.state.collectAsStateWithLifecycle()
    val dialog = rememberDialogState()

    // --- handlers ---
    val mediaHandler = rememberMediaHandler(
        context = context,
        scope = scope,
        onSendMessage = onSendMessage,
        onModeSwitch = controller::dismissAll
    )
    val locationHandler = rememberLocationHandler(onSendMessage)
    val fileHandler = rememberFileHandler(onSendMessage)

    // --- launchers ---
    val mediaLaunchers = rememberMediaLaunchers(mediaHandler)
    val locationLauncher = rememberLocationLauncher(locationHandler)
    val fileLauncher = rememberFileLauncher(fileHandler)

    // --- 路由表 ---
    val actionHandler = rememberActionHandler(
        mediaLaunchers = mediaLaunchers,
        locationLauncher = locationLauncher,
        fileLauncher = fileLauncher,
        onSendMessage = onSendMessage,
        onLaunchCall = onLaunchCall
    )

    return remember(controller, actionHandler, soundPlayer) {
        InputBarActions(
            // 文本
            onTextChange = controller::updateText,
            onLineCountChange = controller::updateLineCount,
            onSendText = {
                if (state.inputText.isNotBlank()) {
                    onSendMessage(MessageContent.Text(state.inputText), null)
                    controller.clearInput()
                } else {
                    dialog.show("提示", "不能发送空白消息", onCancel = null)
                }
            },
            onInsertEmoji = controller::insertEmoji,
            onEmojiBackspace = controller::handleEmojiBackspace,
            onToggleExpand = controller::toggleExpand,

            // 模式切换
            onSwitchMode = controller::switchMode,
            onSwitchToVoice = { controller.switchMode(InputMode.Voice) },
            onSwitchToText = {
                controller.switchToTextMode()
                scope.launch {
                    delay(50)
                    controller.focusRequester.requestFocus()
                }
            },

            // 媒体 / 更多
            onMoreAction = actionHandler::handleAction,
            onVoiceSend = { path, duration ->
                onSendMessage(MessageContent.Voice(path, duration)) {
                    soundPlayer.play(R.raw.after_upload_voice)
                }
            },
            onSpeechResult = { text ->
                val current = state.inputText
                controller.updateText(
                    if (current.isNotEmpty()) "$current，$text" else text
                )
            },
            onSendSticker = { sticker -> onSendMessage(sticker, null) },

            // 透传
            onLaunchCall = onLaunchCall
        )
    }
}