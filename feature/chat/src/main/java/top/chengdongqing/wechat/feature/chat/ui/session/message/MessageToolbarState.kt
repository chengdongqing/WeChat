package top.chengdongqing.wechat.feature.chat.ui.session.message

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import top.chengdongqing.wechat.core.data.model.ChatMessage

/**
 * 消息工具条显示状态
 */
data class MessageToolbarState(
    val visible: Boolean = false,
    val message: ChatMessage? = null,
    val bubblePosition: Offset = Offset.Zero,
    val bubbleHeight: Float = 0f,
    val actions: List<MessageAction> = emptyList(),
    val selectedText: String? = null,
    val textSelection: TextRange? = null,
    val isTextSelectionDragging: Boolean = false
)
