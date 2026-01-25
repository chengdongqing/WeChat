package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.utils.NativeFocusRequester

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

    // 重写 toString 方便日志调试，否则打印出来只是 ChatInputMode(value=0)
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
    initialMode: ChatInputMode,
    private val focusRequester: NativeFocusRequester,
    private val keyboardController: SoftwareKeyboardController?,
    private val scope: CoroutineScope
) {
    private val _inputMode = mutableStateOf(initialMode)
    val inputMode: State<ChatInputMode> = _inputMode

    /**
     * 切换输入模式
     */
    fun switchMode(target: ChatInputMode, showKeyboard: Boolean = true) {
        val oldMode = _inputMode.value
        if (oldMode == target) return

        _inputMode.value = target

        when (target) {
            ChatInputMode.TEXT -> {
                if (showKeyboard) {
                    focusRequester.requestFocus()
                } else {
                    focusRequester.clearFocus()
                }
            }

            ChatInputMode.EMOJI -> {
                focusRequester.clearFocus()
                keyboardController?.hide()

                scope.launch {
                    delay(200)
                    focusRequester.requestFocus(showKeyboard = false)
                }
            }

            ChatInputMode.VOICE, ChatInputMode.MORE -> {
                focusRequester.clearFocus()
                keyboardController?.hide()
            }
        }
    }

    /**
     * 仅同步状态，不触发交互
     */
    internal fun syncMode(target: ChatInputMode) {
        _inputMode.value = target
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberInputModeController(
    focusRequester: NativeFocusRequester,
    initialMode: ChatInputMode = ChatInputMode.TEXT
): InputModeController {
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    val controller = remember {
        InputModeController(initialMode, focusRequester, keyboardController, scope)
    }
    val inputMode by controller.inputMode

    // 键盘弹出时自动设置为文本模式
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            controller.syncMode(ChatInputMode.TEXT)
        }
    }

    // 返回时面板展开则执行关闭面板
    BackHandler(inputMode.isPanelMode) {
        controller.switchMode(ChatInputMode.TEXT, showKeyboard = false)
    }

    return controller
}