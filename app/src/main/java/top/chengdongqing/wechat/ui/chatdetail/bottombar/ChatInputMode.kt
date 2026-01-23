package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    private val focusRequester: NativeFocusRequester,
    private val keyboardController: SoftwareKeyboardController?
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
        focusRequester.clearFocus()
        keyboardController?.hide()
    }

    private fun showKeyboard() {
        inputMode.value = ChatInputMode.TEXT
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberInputModeController(
    focusRequester: NativeFocusRequester,
    initialMode: ChatInputMode = ChatInputMode.TEXT
): InputModeController {
    val inputMode = remember { mutableStateOf(initialMode) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(isImeVisible) {
        // 如果键盘弹起了，强制隐藏自定义面板
        if (isImeVisible) {
            inputMode.value = ChatInputMode.TEXT
        }
        // 显示表情面板时，显示输入框的光标
        else if (inputMode.value.isEmoji) {
            focusRequester.requestFocus(showKeyboard = false)
        }
    }

    return remember {
        InputModeController(inputMode, focusRequester, keyboardController)
    }
}