package top.chengdongqing.wechat.feature.chat.ui.session.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.DialogManager
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.navigation.LocalContactPickerLauncher
import top.chengdongqing.wechat.feature.chat.domain.model.InputMode
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberActionHandler
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberFileHandler
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberFileLauncher
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberLocationHandler
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberLocationLauncher
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberMediaHandler
import top.chengdongqing.wechat.feature.chat.ui.session.input.handler.rememberMediaLaunchers
import kotlin.time.Duration.Companion.milliseconds

/**
 * 输入栏 Actions 组装入口
 *
 * 将所有 handler / launcher / controller 编排成 [InputBarActions]，
 * 调用方（InputBar）只需拿到这一个对象，不感知内部依赖。
 */
@Composable
fun rememberInputBarActions(
    controller: InputBarController,
    onSendMessage: (MessageContent) -> Unit,
    onLaunchCall: (CallType) -> Unit,
    onStartLive: () -> Unit,
    onShareLiveLocation: () -> Unit,
    onOpenFavorites: () -> Unit
): InputBarActions {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val privateFileManager = hiltViewModel<InputBarViewModel>().privateFileManager

    // --- handlers ---
    val mediaHandler = rememberMediaHandler(
        privateFileManager = privateFileManager,
        onSendMessage = onSendMessage,
        onModeChange = controller::dismissAll
    )
    val locationHandler = rememberLocationHandler(privateFileManager, onSendMessage)
    val fileHandler = rememberFileHandler(privateFileManager, onSendMessage)

    // --- launchers ---
    val mediaLaunchers = rememberMediaLaunchers(mediaHandler)
    val onPickLocation = rememberLocationLauncher(locationHandler)
    val fileLauncher = rememberFileLauncher(fileHandler)
    val pickContact = LocalContactPickerLauncher.current.rememberLauncher { contacts ->
        scope.launch { fileHandler.handleContactSelection(contacts.first()) }
    }

    // --- 路由表 ---
    val actionHandler = rememberActionHandler(
        mediaLaunchers = mediaLaunchers,
        fileLauncher = fileLauncher,
        onOpenFilePicker = fileLauncher.pickFile,
        privateFileManager = privateFileManager,
        pickContact = { pickContact(1) },
        onPickLocation = onPickLocation,
        onShareLiveLocation = onShareLiveLocation,
        onLaunchCall = onLaunchCall,
        onSelectMusic = controller::toggleMusic,
        onStartLive = onStartLive,
        onOpenFavorites = onOpenFavorites,
        onSendMessage = onSendMessage
    )

    return remember(controller, actionHandler) {
        InputBarActions(
            // 文本
            onTextChange = controller::updateText,
            onLineCountChange = controller::updateLineCount,
            onSendText = {
                val inputText = controller.state.value.inputText
                if (inputText.isNotBlank()) {
                    onSendMessage(MessageContent.Text(inputText))
                    controller.clearInput()
                } else {
                    DialogManager.show(
                        title = resources.getString(R.string.msg_empty_message_title),
                        content = resources.getString(R.string.msg_empty_message_content),
                        onCancel = null
                    )
                }
            },
            onInsertEmoji = controller::insertEmoji,
            onEmojiBackspace = controller::handleEmojiBackspace,
            onToggleExpand = controller::toggleExpand,
            onToggleMusic = controller::toggleMusic,
            // 模式切换
            onSwitchMode = controller::switchMode,
            onSwitchToVoice = { controller.switchMode(InputMode.Voice) },
            onSwitchToText = {
                controller.switchToTextMode()
                scope.launch {
                    delay(50.milliseconds)
                    controller.focusRequester.requestFocus()
                }
            },
            // 媒体 / 更多
            onMoreAction = actionHandler::handleAction,
            onSpeechResult = { text ->
                val current = controller.state.value.inputText
                controller.updateText(if (current.isNotEmpty()) "$current，$text" else text)
            },
            onSendMessage = onSendMessage,
            // 透传
            onLaunchCall = onLaunchCall
        )
    }
}
