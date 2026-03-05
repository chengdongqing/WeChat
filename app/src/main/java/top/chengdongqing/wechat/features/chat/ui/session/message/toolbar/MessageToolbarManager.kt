package top.chengdongqing.wechat.features.chat.ui.session.message.toolbar

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.util.copyToClipboard
import top.chengdongqing.wechat.core.util.isWithinSeconds
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.features.chat.domain.model.ChatMessage
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageAction
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageToolbarState
import top.chengdongqing.wechat.features.chat.ui.session.message.MessageUiEvent

/**
 * 消息工具条状态管理器
 *
 * - 长按弹出工具条
 * - 文本选区变化跟踪
 * - 工具条操作分发
 * - 可用操作列表计算
 */
class MessageToolbarManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val uiEvent: MutableSharedFlow<MessageUiEvent>,
    private val onRecallMessage: (String) -> Unit,
    private val onToggleSpeaker: () -> Unit,
    private val onSaveFile: (ChatMessage) -> Unit,
    private val onMultiSelect: (String) -> Unit
) {
    private val _state = MutableStateFlow(MessageToolbarState())
    val state = _state.asStateFlow()

    /**
     * 处理消息长按
     *
     * 文本消息自动全选文字并记录选中文本，
     * 其他消息仅弹出工具条。
     */
    fun onLongPress(
        message: ChatMessage,
        bubblePosition: Offset,
        bubbleHeight: Float,
        isSpeakerOn: Boolean
    ) {
        val actions = buildActions(message, isSpeakerOn)

        val textState = if (message.content is MessageContent.Text) {
            val text = message.content.text
            Triple(TextRange(0, text.length), text, true)
        } else {
            Triple(null, null, false)
        }

        _state.update {
            it.copy(
                visible = true,
                message = message,
                bubblePosition = bubblePosition,
                bubbleHeight = bubbleHeight,
                actions = actions,
                textSelection = textState.first,
                selectedText = textState.second
            )
        }
    }

    /**
     * 更新文本选区
     */
    fun onTextSelectionChange(selection: TextRange) {
        val content = _state.value.message?.content as? MessageContent.Text ?: return
        val selectedText = content.text.substring(selection.start, selection.end)

        _state.update {
            it.copy(textSelection = selection, selectedText = selectedText)
        }
    }

    /**
     * 关闭工具条，重置所有状态
     */
    fun dismiss() {
        _state.update { MessageToolbarState() }
    }

    /**
     * 处理工具条按钮点击
     */
    fun onAction(action: MessageAction) {
        val currentState = _state.value
        val message = currentState.message ?: return

        when (action) {
            MessageAction.Copy -> {
                currentState.selectedText?.let {
                    context.copyToClipboard(it, "message")
                    context.showToast("已复制")
                }
            }

            MessageAction.Delete -> {
                scope.launch {
                    uiEvent.emit(MessageUiEvent.ShowDeleteConfirm(message.id))
                }
            }

            MessageAction.Forward -> {
                scope.launch {
                    uiEvent.emit(MessageUiEvent.ForwardMessage(message.id))
                }
            }

            MessageAction.Recall -> onRecallMessage(message.id)

            MessageAction.SpeakerMode,
            MessageAction.EarpieceMode -> onToggleSpeaker()

            MessageAction.Download -> onSaveFile(message)

            MessageAction.MultiSelect -> onMultiSelect(message.id)

            else -> {}
        }

        dismiss()
    }

    /**
     * 根据消息类型和状态，生成可用的操作列表
     */
    private fun buildActions(
        message: ChatMessage,
        isSpeakerOn: Boolean
    ): List<MessageAction> {
        val deleteOrRecall = run {
            val canRecall = run {
                /**
                 * 可以撤回的条件：
                 * 1. 是我发的
                 * 2. 不是发给自己的
                 * 3. 5分钟以内的
                 * 4. 发送成功的
                 */
                val isSelfSession = message.sessionId == message.senderId
                message.isFromMe && message.timestamp.isWithinSeconds() && !isSelfSession && !message.isFailed
            }
            if (canRecall) MessageAction.Recall else MessageAction.Delete
        }

        return buildList {
            when (message.content) {
                is MessageContent.Text -> {
                    add(MessageAction.Copy)
                    add(MessageAction.Forward)
                    add(MessageAction.Favorite)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                }

                is MessageContent.Voice -> {
                    add(if (isSpeakerOn) MessageAction.EarpieceMode else MessageAction.SpeakerMode)
                    add(MessageAction.Forward)
                    add(MessageAction.Favorite)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(MessageAction.Download)
                }

                is MessageContent.Sticker -> {
                    add(MessageAction.Forward)
                    add(deleteOrRecall)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Download)
                }

                is MessageContent.Call -> {
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(MessageAction.Delete)
                }

                is MessageContent.ContactCard -> {
                    add(MessageAction.Forward)
                    add(MessageAction.Quote)
                    add(MessageAction.Remind)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                }

                is MessageContent.Image,
                is MessageContent.Video,
                is MessageContent.Location,
                is MessageContent.Favorite,
                is MessageContent.File -> {
                    add(MessageAction.Forward)
                    add(MessageAction.Favorite)
                    add(MessageAction.Quote)
                    add(deleteOrRecall)
                    add(MessageAction.MultiSelect)
                    add(MessageAction.Remind)
                    add(MessageAction.Download)
                }

                else -> {}
            }
        }
    }
}