package top.chengdongqing.wechat.feature.chat.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.feature.chat.ui.session.util.VoicePlaybackState

/**
 * 聊天会话上下文
 */
data class ChatSessionContext(
    val title: String,
    val isSelf: Boolean,
    val isGroup: Boolean,
    val playingMessageId: String?,
    val voicePlaybackState: VoicePlaybackState,
    val onVoiceSeek: (String, Float) -> Unit,
    val onVoiceSpeedToggle: (String) -> Unit,
    val onVoiceStop: () -> Unit,
    val onRetrySend: (messageId: String) -> Unit,
    val onRequestAddFriend: () -> Unit,
    val onContact: (isPeer: Boolean) -> Unit,
    val onWebView: (url: String) -> Unit,
    val onLive: (liveId: String, isHost: Boolean, hostId: String) -> Unit,
    val activeLiveLocationRoomId: String?,
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
    onContact: (isPeer: Boolean) -> Unit,
    onRequestAddFriend: () -> Unit,
    onWebView: (url: String) -> Unit,
    onLive: (liveId: String, isHost: Boolean, hostId: String) -> Unit
): ChatSessionContext {
    val scope = rememberCoroutineScope()
    val playingMessageId by viewModel.playingMessageId.collectAsStateWithLifecycle()
    val voicePlaybackState by viewModel.voicePlaybackState.collectAsStateWithLifecycle()
    val liveLocationRoom by viewModel.liveLocationRoom.collectAsStateWithLifecycle()

    return remember(playingMessageId, voicePlaybackState, uiState.isSelf, liveLocationRoom) {
        ChatSessionContext(
            title = uiState.title,
            isSelf = uiState.isSelf == true,
            isGroup = viewModel.isGroupSession,
            playingMessageId = playingMessageId,
            voicePlaybackState = voicePlaybackState,
            onVoiceSeek = viewModel::seekVoice,
            onVoiceSpeedToggle = viewModel::toggleVoiceSpeed,
            onVoiceStop = viewModel::stopVoice,
            onRetrySend = { viewModel.retrySend(it) },
            onRequestAddFriend = {
                scope.launch {
                    viewModel.prepareRequestAddFriend().onSuccess {
                        onRequestAddFriend()
                    }
                }
            },
            onContact = onContact,
            onWebView = onWebView,
            onLive = onLive,
            activeLiveLocationRoomId = liveLocationRoom.roomId.takeIf {
                liveLocationRoom.isActive
            },
            onCancelTransfer = viewModel::cancelTransfer,
            onPauseTransfer = viewModel::pauseTransfer,
            onResumeTransfer = viewModel::resumeTransfer,
            onReeditMessage = viewModel::reeditMessage
        )
    }
}
