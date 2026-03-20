package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.chat.ui.session.message.ChatSessionUiState

/**
 * 聊天会话上下文
 */
data class ChatSessionContext(
    val title: String,
    val isSelf: Boolean,
    val playingMessageId: String?,
    val onVoiceStop: () -> Unit,
    val onRetrySend: (messageId: String) -> Unit,
    val onNavigateToRequestAddFriend: () -> Unit,
    val onNavigateToContact: (isPeer: Boolean) -> Unit,
    val onNavigateToWebView: (url: String) -> Unit,
    val onCancelTransfer: (messageId: String) -> Unit,
    val onPauseTransfer: (messageId: String) -> Unit,
    val onResumeTransfer: (messageId: String) -> Unit,
    val onReeditMessage: (text: String) -> Unit
)

val LocalChatSessionContext = compositionLocalOf<ChatSessionContext?> { null }

/**
 * 创建聊天会话上下文
 */
@Composable
fun rememberChatSessionContext(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    onNavigateToContact: (isPeer: Boolean) -> Unit,
    onNavigateToRequestAddFriend: () -> Unit,
    onNavigateToWebView: (url: String) -> Unit
): ChatSessionContext {
    val scope = rememberCoroutineScope()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()

    return remember(playingMessageId, uiState.isSelf) {
        ChatSessionContext(
            title = uiState.title,
            isSelf = uiState.isSelf == true,
            playingMessageId = playingMessageId,
            onVoiceStop = viewModel::stopVoice,
            onRetrySend = { viewModel.retrySend(it) },
            onNavigateToRequestAddFriend = {
                scope.launch {
                    viewModel.prepareRequestAddFriend().onSuccess {
                        onNavigateToRequestAddFriend()
                    }
                }
            },
            onNavigateToContact = onNavigateToContact,
            onNavigateToWebView = onNavigateToWebView,
            onCancelTransfer = viewModel::cancelTransfer,
            onPauseTransfer = viewModel::pauseTransfer,
            onResumeTransfer = viewModel::resumeTransfer,
            onReeditMessage = viewModel::reeditMessage
        )
    }
}