package top.chengdongqing.wechat.feature.chat.ui.session

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageQuote
import top.chengdongqing.wechat.core.model.Contact
import top.chengdongqing.wechat.core.model.UserProfile
import top.chengdongqing.wechat.core.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.feature.chat.ui.location.LiveLocationRoomState
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageToolbarState
import top.chengdongqing.wechat.feature.chat.ui.session.util.VoicePlaybackState

/**
 * 会话页面 UI 状态
 */
@Immutable
data class ChatUiState(
    val chatId: String = "",
    val chatType: ChatType = ChatType.Single,
    val chatTitle: String? = null,
    val myUserInfo: UserProfile? = null,
    val peerUserInfo: Contact? = null,
    val isInfoLoaded: Boolean = false,
    val isFullscreenLoading: Boolean = false,
    val backgroundImagePath: String? = null,
    val isMuted: Boolean = false,
    val isTemporary: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isSendButtonOn: Boolean = true,
    val isOnline: Boolean = false,
    val isE2EActive: Boolean = false,
    val draftMessage: String? = null,
    val isSelectMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val totalUnreadCount: Int = 0,
    val connectionMode: ConnectionMode = ConnectionMode.WiFiLan,
    val connectionRequired: ConnectionRequiredEvent? = null,
    val isConnected: () -> Boolean = { false },
    val isBluetoothDeviceBonded: suspend () -> Boolean = { false },
    val voicePlaybackState: VoicePlaybackState = VoicePlaybackState(),
    val pendingQuote: MessageQuote? = null,
    val streamingAiMessage: StreamingAiMessage? = null,
    val toolbarState: MessageToolbarState = MessageToolbarState(),
    val liveLocationRoom: LiveLocationRoomState = LiveLocationRoomState(""),
    val messagePaging: Flow<PagingData<ChatMessage>> = emptyFlow()
) {
    val selectedCount: Int
        get() = selectedMessageIds.size
}

enum class ChatType {
    Single, Group, Self, Ai;

    val isSelf: Boolean
        get() = this == Self

    val isAi: Boolean
        get() = this == Ai

    val isSelfOrAi: Boolean
        get() = isSelf || isAi
}