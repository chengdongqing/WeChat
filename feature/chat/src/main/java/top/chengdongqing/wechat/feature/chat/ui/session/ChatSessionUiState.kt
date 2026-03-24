package top.chengdongqing.wechat.feature.chat.ui.session

/**
 * 会话页面 UI 状态
 */
data class ChatSessionUiState(
    val title: String = "",
    val peerId: String? = null,
    val peerAvatar: String? = null,
    val myId: String? = null,
    val myAvatar: String? = null,
    val isSelf: Boolean? = null,
    val isFullscreenLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val backgroundPath: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isSendButtonOn: Boolean = true,
    val isOnline: Boolean = false,
    val draftMessage: String? = null,
    val isSelectMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet()
) {
    val selectedCount: Int
        get() = selectedMessageIds.size
}