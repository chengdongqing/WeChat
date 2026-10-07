package top.chengdongqing.wechat.feature.chat.ui.session

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.feature.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.feature.chat.ui.session.message.MultiMessageAction

sealed interface ChatUiIntent {
    // 进入页面回调
    data object OnEnter : ChatUiIntent

    // 离开页面回调
    data object OnLeave : ChatUiIntent

    // 处理工具栏点击事件
    data class HandleToolbarAction(val action: MessageAction) : ChatUiIntent
    data class HandleMultiSelectAction(val action: MultiMessageAction) : ChatUiIntent

    // 发送消息
    data class SendMessage(val content: MessageContent) : ChatUiIntent

    // 重发消息
    data class RetrySend(val messageId: String) : ChatUiIntent

    // 保存草稿消息
    data class SaveDraft(val text: String) : ChatUiIntent

    // 播放/暂停语音
    data class ToggleVoicePlay(
        val messageId: String,
        val localPath: String,
        val durationMs: Long
    ) : ChatUiIntent

    // 设置语音播放进度
    data class SeekVoice(val messageId: String, val fraction: Float) : ChatUiIntent

    // 切换语音播放速度
    data class ToggleVoiceSpeed(val messageId: String) : ChatUiIntent

    // 切换听筒/扬声器
    data object ToggleSpeaker : ChatUiIntent

    data class SelectMessage(val messageId: String) : ChatUiIntent
    data object ExitSelectMode : ChatUiIntent
    data object ClearUnread : ChatUiIntent

    data class MessagesUpdated(val messages: List<ChatMessage>) : ChatUiIntent
    data class FinishAiStreamHandoff(val messageId: String) : ChatUiIntent
    data object DismissToolbar : ChatUiIntent
    data class UpdateTextSelection(val selection: TextRange) : ChatUiIntent
    data class UpdateTextSelectionDragging(val isDragging: Boolean) : ChatUiIntent
    data class UpdateTextSelectionBounds(val position: Offset, val height: Float) : ChatUiIntent
    data class QuoteMessage(val message: ChatMessage) : ChatUiIntent
    data object CancelQuote : ChatUiIntent
    data class ForwardMessage(val message: ChatMessage) : ChatUiIntent
    data class MessageClicked(val message: ChatMessage) : ChatUiIntent
    data class MessageLongPressed(
        val message: ChatMessage,
        val position: Offset,
        val height: Float
    ) : ChatUiIntent

    data class SendEditedImage(val uri: Uri, val targetChatIds: Set<String>) : ChatUiIntent
    data class FavoriteEditedImage(val uri: Uri) : ChatUiIntent
    data class SaveEditedImage(val uri: Uri) : ChatUiIntent
    data object StopVoice : ChatUiIntent
    data class CancelTransfer(val messageId: String) : ChatUiIntent
    data class PauseTransfer(val messageId: String) : ChatUiIntent
    data class ResumeTransfer(val messageId: String) : ChatUiIntent
    data class ReeditMessage(val text: String) : ChatUiIntent
    data object ShareLiveLocation : ChatUiIntent
    data class DeleteMessage(val messageId: String?) : ChatUiIntent
    data object SaveSelectedFiles : ChatUiIntent
    data class ForwardMessages(
        val targetChatIds: Set<String>,
        val messageId: String? = null,
        val merged: Boolean = false
    ) : ChatUiIntent

    data object RequestAddFriend : ChatUiIntent
}
