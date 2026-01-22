package top.chengdongqing.wechat.ui.chatdetail

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * 输入模式枚举
 *
 * 参考官方的`ImeAction`等：value class相比enum性能更好，消除了对象分配开销。
 */
@Immutable // 告诉编译器这个值永远不会改变
@JvmInline // 内联类，运行时只占用一个 Int 的空间
value class ChatInputMode private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        val TEXT = ChatInputMode(0)   // 文本模式
        val VOICE = ChatInputMode(1)  // 语音模式
        val EMOJI = ChatInputMode(2)  // 表情面板
        val MORE = ChatInputMode(3)   // 更多面板
    }

    // 快捷判断
    val isText get() = this == TEXT
    val isVoice get() = this == VOICE
    val isEmoji get() = this == EMOJI
    val isMore get() = this == MORE
    val isPanelMode get() = this == EMOJI || this == MORE

    // 重写 toString 方便日志调试，否则打印出来只是 PanelState(value=0)
    override fun toString(): String = when (this) {
        TEXT -> "TEXT"
        VOICE -> "VOICE"
        EMOJI -> "EMOJI"
        MORE -> "MORE"
        else -> "Unknown"
    }
}

/**
 * 输入模式控制器
 */
class InputModeController(
    val inputMode: MutableState<ChatInputMode>,
    private val focusRequester: FocusRequester,
    private val keyboardController: SoftwareKeyboardController?,
    private val focusManager: FocusManager
) {
    fun switchMode(target: ChatInputMode) {
        inputMode.value = target

        if (target.isText) {
            showKeyboard()
        } else {
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    private fun showKeyboard() {
        inputMode.value = ChatInputMode.TEXT
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberInputModeController(
    focusRequester: FocusRequester,
    initialMode: ChatInputMode = ChatInputMode.TEXT
): InputModeController {
    val inputMode = remember { mutableStateOf(initialMode) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    // 如果键盘弹起了，强制隐藏自定义面板
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            inputMode.value = ChatInputMode.TEXT
        }
    }

    return remember(focusRequester, keyboardController, focusManager) {
        InputModeController(inputMode, focusRequester, keyboardController, focusManager)
    }
}