package top.chengdongqing.wechat.feature.chat.ui.session.message

import android.net.Uri

import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.model.CallType

/**
 * 会话页 UI 事件
 */
sealed class MessageUiEvent {
    /** 显示删除确认对话框 */
    data class ShowDeleteConfirm(val messageId: String? = null) : MessageUiEvent()

    /** 显示保存确认对话框 */
    data object ShowDownloadConfirm : MessageUiEvent()

    /** 转发消息 */
    data class ForwardMessage(val messageId: String? = null) : MessageUiEvent()

    /** 使用公共图片编辑器编辑消息图片。 */
    data class EditImage(val uri: Uri) : MessageUiEvent()

    /** 重新编辑消息 */
    data class ReeditMessage(val text: String) : MessageUiEvent()

    /** 预览文件 */
    data class PreviewFile(val messageId: String) : MessageUiEvent()

    /** 预览音乐 */
    data class PreviewMusic(val messageId: String, val trackName: String) : MessageUiEvent()

    /** 在聊天页面内预览媒体，以支持共享元素转场 */
    data class PreviewMedia(
        val medias: List<MediaItem>,
        val messageIds: List<String>,
        val initialIndex: Int
    ) : MessageUiEvent()

    /** 调起通话 */
    data class LaunchCall(val callType: CallType) : MessageUiEvent()

    /** 跳转到联系人详情 */
    data class NavigateToContact(val contactId: String) : MessageUiEvent()
    data object NavigateToLiveLocation : MessageUiEvent()
    data class OpenChatHistory(val content: MessageContent.ChatHistory) : MessageUiEvent()
}
