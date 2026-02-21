package top.chengdongqing.wechat.features.chat.ui.session.input

import androidx.compose.runtime.Stable
import top.chengdongqing.wechat.core.designsystem.model.Emoji

/**
 * 输入栏UI状态
 */
@Stable
data class InputBarState(
    val inputText: String = "",
    val inputMode: InputMode = InputMode.Text,
    val isExpanded: Boolean = false,
    val lineCount: Int = 1,
    val recentEmojis: List<Emoji> = emptyList()
) {
    /**
     * 是否显示发送按钮
     */
    val shouldShowSendButton: Boolean
        get() = inputText.isNotBlank()

    /**
     * 是否显示全屏输入按钮
     */
    val shouldShowExpandButton: Boolean
        get() = lineCount >= 3 && !inputMode.isVoice
}