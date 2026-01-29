package top.chengdongqing.wechat.ui.chat.session.input

import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.components.emojitextfield.NativeFocusRequester

class InputHandler(
    private val text: State<String>,
    private val focusRequester: NativeFocusRequester,
    private val scope: CoroutineScope,
    private val onTextChange: (String) -> Unit,
) {
    private val currentText
        get() = text.value

    /**
     * 插入表情
     */
    fun insertEmoji(emojiDescription: String) {
        val insertText = "[$emojiDescription]"
        val cursorIndex = focusRequester.selectionStart
        val newText = StringBuilder(currentText).insert(cursorIndex, insertText).toString()
        onTextChange(newText)

        // 计算新的光标位置
        val newCursorIndex = cursorIndex + insertText.length
        scope.launch {
            delay(16)
            focusRequester.setSelection(newCursorIndex)
        }
    }

    /**
     * 处理表情面板内的回退
     */
    fun handleEmojiBackspace() {
        handleTextBackspace(currentText, focusRequester.selectionStart) { newString, newPos ->
            onTextChange(newString)

            // 同步光标
            focusRequester.post {
                focusRequester.setSelection(newPos)
            }
        }
    }

    /**
     * 处理文本删除
     */
    private fun handleTextBackspace(
        text: String,
        selectionStart: Int,
        onChange: (newText: String, newCursorPos: Int) -> Unit
    ) {
        if (selectionStart <= 0) return

        val textBefore = text.take(selectionStart)
        val textAfter = text.substring(selectionStart)

        // 匹配光标左侧紧邻的 "[xxx]"
        val match = EMOJI_BACKSPACE_PATTERN_REGEX.find(textBefore)

        if (match != null) {
            // A：光标前是表情块，整体删除
            val newText = textBefore.removeRange(match.range) + textAfter
            val newCursorPos = match.range.first
            onChange(newText, newCursorPos)
        } else {
            // B：普通文本，只删一个字符
            val newText = textBefore.dropLast(1) + textAfter
            val newCursorPos = selectionStart - 1
            onChange(newText, newCursorPos)
        }
    }
}

val EMOJI_BACKSPACE_PATTERN_REGEX = Regex("\\[[^\\[\\]]+]$") // 以 [ 开头，中间包含非括号字符，以 ] 结尾，且必须紧贴末尾($)