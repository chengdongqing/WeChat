package top.chengdongqing.wechat.features.chat.ui.session.message

import top.chengdongqing.wechat.features.call.domain.model.CallType

/**
 * 会话页 UI 事件（单次触发，驱动 dialog / navigation 等副作用）
 */
sealed class MessageUiEvent {
    /** 显示删除确认对话框 */
    data class ShowDeleteConfirm(val messageId: String? = null) : MessageUiEvent()

    /** 显示保存确认对话框 */
    data object ShowDownloadConfirm : MessageUiEvent()

    /** 转发消息 */
    data class ForwardMessage(val messageId: String? = null) : MessageUiEvent()

    /** 重新编辑消息 */
    data class ReeditMessage(val text: String) : MessageUiEvent()

    /** 预览文件 */
    data class PreviewFile(val messageId: String) : MessageUiEvent()

    /** 预览音乐 */
    data class PreviewMusic(val messageId: String, val trackName: String) : MessageUiEvent()

    /** 调起通话 */
    data class LaunchCall(val callType: CallType) : MessageUiEvent()

    /** 跳转到联系人详情 */
    data class NavigateToContact(val contactId: String) : MessageUiEvent()
}
