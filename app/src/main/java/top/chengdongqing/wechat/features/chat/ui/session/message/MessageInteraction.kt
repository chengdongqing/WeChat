package top.chengdongqing.wechat.features.chat.ui.session.message

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.features.call.domain.model.CallType
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
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val backgroundPath: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
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
    @param:DrawableRes val icon: Int,
    val label: String
) {
    Copy(R.drawable.ic_copy_filled, "复制"),
    Delete(R.drawable.ic_delete_filled, "删除"),
    Forward(R.drawable.ic_forward_filled, "转发"),
    Favorite(R.drawable.ic_favorites_filled, "收藏"),
    Remind(R.drawable.ic_bell_filled, "提醒"),
    Recall(R.drawable.ic_recall_outlined, "撤回"),
    MultiSelect(R.drawable.ic_multi_select_outlined, "多选"),
    SpeakerMode(R.drawable.ic_speaker_filled, "扬声器"),
    EarpieceMode(R.drawable.ic_ear_filled, "听筒"),
    Quote(R.drawable.ic_quote_filled, "引用"),
    Download(R.drawable.ic_download_filled, "保存")
}

/**
 * 多选时消息操作类型枚举
 */
enum class MultiMessageAction(
    @param:DrawableRes val icon: Int,
    val label: String
) {
    Forward(R.drawable.ic_forward_outlined, "转发"),
    Favorite(R.drawable.ic_favorites_outlined, "收藏"),
    Delete(R.drawable.ic_delete_outlined, "删除"),
    Download(R.drawable.ic_download_outlined, "保存"),
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
     * 调起通话
     */
    data class LaunchCall(val callType: CallType) : MessageUiEvent()
}