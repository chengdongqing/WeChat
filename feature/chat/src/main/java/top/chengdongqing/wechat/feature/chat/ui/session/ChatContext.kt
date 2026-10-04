package top.chengdongqing.wechat.feature.chat.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import top.chengdongqing.wechat.core.navigation.ScreenRoute
import top.chengdongqing.wechat.feature.chat.ui.session.util.VoicePlaybackState

/**
 * 聊天会话上下文
 */
data class ChatContext(
    val title: String,
    val isSelf: Boolean,
    val isGroup: Boolean,
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

val LocalChatContext = compositionLocalOf<ChatContext?> { null }

/**
 * 创建聊天会话上下文
 */
@Composable
fun rememberChatContext(
    uiState: ChatUiState,
    onIntent: (ChatUiIntent) -> Unit,
    onNavigate: (ScreenRoute) -> Unit
): ChatContext {
    val voicePlaybackState = uiState.voicePlaybackState
    val liveLocationRoom = uiState.liveLocationRoom

    return remember(
        uiState.chatId,
        uiState.chatTitle,
        uiState.chatType,
        uiState.peerUserInfo?.id,
        uiState.myUserInfo?.id,
        voicePlaybackState,
        liveLocationRoom,
        onIntent
    ) {
        ChatContext(
            title = uiState.chatTitle.orEmpty(),
            isSelf = uiState.chatType == ChatType.Self,
            isGroup = uiState.chatType == ChatType.Group,
            voicePlaybackState = voicePlaybackState,
            onVoiceSeek = { messageId, fraction ->
                onIntent(
                    ChatUiIntent.SeekVoice(
                        messageId,
                        fraction
                    )
                )
            },
            onVoiceSpeedToggle = { messageId -> onIntent(ChatUiIntent.ToggleVoiceSpeed(messageId)) },
            onVoiceStop = { onIntent(ChatUiIntent.StopVoice) },
            onRetrySend = { onIntent(ChatUiIntent.RetrySend(it)) },
            onRequestAddFriend = { onIntent(ChatUiIntent.RequestAddFriend) },
            onContact = { isPeer ->
                val contactId = if (isPeer) {
                    uiState.peerUserInfo?.id ?: uiState.chatId
                } else {
                    uiState.myUserInfo?.id.orEmpty()
                }
                onNavigate(ScreenRoute.ContactDetail(contactId))
            },
            onWebView = { onNavigate(ScreenRoute.WebView(it)) },
            onLive = { liveId, isHost, hostId ->
                onNavigate(ScreenRoute.LiveRoom(uiState.chatId, liveId, isHost, hostId))
            },
            activeLiveLocationRoomId = liveLocationRoom.roomId.takeIf {
                liveLocationRoom.isActive
            },
            onCancelTransfer = { onIntent(ChatUiIntent.CancelTransfer(it)) },
            onPauseTransfer = { onIntent(ChatUiIntent.PauseTransfer(it)) },
            onResumeTransfer = { onIntent(ChatUiIntent.ResumeTransfer(it)) },
            onReeditMessage = { onIntent(ChatUiIntent.ReeditMessage(it)) }
        )
    }
}
