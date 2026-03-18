package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.features.call.model.CallType
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage

/**
 * 会话基础状态
 */
data class ChatSessionUiState(
    val title: String = "",
    val peerId: String? = null,
    val peerAvatar: String? = null,
    val myId: String? = null,
    val myAvatar: String? = null,
    val isSelf: Boolean = false,
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

/**
 * 消息操作类型枚举
 */
enum class MessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Copy(R.drawable.ic_copy_filled, R.string.message_action_copy),
    Delete(R.drawable.ic_delete_filled, R.string.message_action_delete),
    Forward(R.drawable.ic_forward_filled, R.string.message_action_forward),
    Favorite(R.drawable.ic_favorites_filled, R.string.message_action_favorite),
    Remind(R.drawable.ic_bell_filled, R.string.message_action_remind),
    Recall(R.drawable.ic_recall_outlined, R.string.message_action_recall),
    MultiSelect(R.drawable.ic_multi_select_outlined, R.string.message_action_multi_select),
    SpeakerMode(R.drawable.ic_speaker_filled, R.string.message_action_speaker),
    EarpieceMode(R.drawable.ic_ear_filled, R.string.message_action_earpiece),
    Quote(R.drawable.ic_quote_filled, R.string.message_action_quote),
    Download(R.drawable.ic_download_filled, R.string.message_action_download)
}

/**
 * 多选时消息操作类型枚举
 */
enum class MultiMessageAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val labelRes: Int
) {
    Forward(R.drawable.ic_forward_outlined, R.string.message_action_forward),
    Favorite(R.drawable.ic_favorites_outlined, R.string.message_action_favorite),
    Delete(R.drawable.ic_delete_outlined, R.string.message_action_delete),
    Download(R.drawable.ic_download_outlined, R.string.message_action_download)
}

/**
 * 工具条显示状态
 */
data class MessageToolbarState(
    val visible: Boolean = false,
    val message: ChatMessage? = null,
    val bubblePosition: Offset = Offset.Zero,
    val bubbleHeight: Float = 0f,
    val actions: List<MessageAction> = emptyList(),
    val selectedText: String? = null,
    val textSelection: TextRange? = null
)

/**
 * UI事件
 */
sealed class MessageUiEvent {
    /**
     * 显示删除确认对话框
     */
    data class ShowDeleteConfirm(val messageId: String? = null) : MessageUiEvent()

    /**
     * 显示保存确认对话框
     */
    object ShowDownloadConfirm : MessageUiEvent()

    /**
     * 转发消息
     */
    data class ForwardMessage(val messageId: String? = null) : MessageUiEvent()

    /**
     * 重新编辑消息
     */
    data class ReeditMessage(val text: String) : MessageUiEvent()

    /**
     * 预览文件
     */
    data class PreviewFile(val messageId: String) : MessageUiEvent()

    /**
     * 预览音乐
     */
    data class PreviewMusic(val messageId: String, val trackName: String) : MessageUiEvent()

    /**
     * 调起通话
     */
    data class LaunchCall(val callType: CallType) : MessageUiEvent()

    /**
     * 跳转到联系人详情
     */
    data class NavigateToContact(val contactId: String) : MessageUiEvent()
}