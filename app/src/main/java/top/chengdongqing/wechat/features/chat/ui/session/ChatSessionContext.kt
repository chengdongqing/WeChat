package top.chengdongqing.wechat.features.chat.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.util.VoicePlayingLifecycle

/**
 * 聊天会话上下文
 */
data class ChatSessionContext(
    val title: String,
    val isSelf: Boolean,
    val mediaList: List<MessageContent.Media>,
    val getMediaIndexOf: (MessageContent.Media) -> Int,
    val playingMessageId: String?,
    val onVoiceToggle: (messageId: String, localPath: String) -> Unit,
    val onVoiceStop: () -> Unit,
    val onRetrySend: (messageId: String) -> Unit,
    val onNavigateToRequestAddFriend: () -> Unit,
    val onPreviewFile: (messageId: String) -> Unit,
    val onLaunchCall: (type: CallType) -> Unit,
    val onNavigateToContact: (isPeer: Boolean) -> Unit,
    val onNavigateToWebView: (url: String) -> Unit,
    val onStopTransfer: (messageId: String) -> Unit
)

val LocalChatSessionContext = compositionLocalOf<ChatSessionContext?> { null }

/**
 * 创建聊天会话上下文
 */
@Composable
fun rememberChatSessionContext(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    onPreviewFile: (messageId: String) -> Unit,
    onLaunchCall: (type: CallType) -> Unit,
    onNavigateToContact: (isPeer: Boolean) -> Unit,
    onNavigateToRequestAddFriend: () -> Unit,
    onNavigateToWebView: (url: String) -> Unit
): ChatSessionContext {
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()

    /**
     * 生命周期感知的语音播放控制
     */
    VoicePlayingLifecycle {
        if (playingMessageId != null) {
            viewModel.stopVoice()
        }
    }

    return remember(mediaList, playingMessageId, uiState.isSelf) {
        ChatSessionContext(
            title = uiState.title,
            isSelf = uiState.isSelf,
            mediaList = mediaList,
            getMediaIndexOf = { content -> mediaList.indexOf(content) },
            playingMessageId = playingMessageId,
            onVoiceToggle = { id, localPath -> viewModel.toggleVoicePlay(id, localPath) },
            onVoiceStop = { if (playingMessageId != null) viewModel.stopVoice() },
            onRetrySend = { viewModel.retrySend(it) },
            onNavigateToRequestAddFriend = {
                viewModel.prepareRequestAddFriend()
                onNavigateToRequestAddFriend()
            },
            onPreviewFile = { onPreviewFile(it) },
            onLaunchCall = onLaunchCall,
            onNavigateToContact = onNavigateToContact,
            onNavigateToWebView = onNavigateToWebView,
            onStopTransfer = viewModel::stopTransfer
        )
    }
}