package top.chengdongqing.wechat.features.chat.ui.session.input

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.core.designsystem.model.Emojis
import top.chengdongqing.wechat.features.chat.data.store.RecentEmojisStore
import top.chengdongqing.wechat.features.chat.ui.session.input.panel.RecentEmojisViewModel

/**
 * 输入栏控制器
 *
 * 融合了输入状态管理、模式切换、键盘控制、焦点管理等功能
 */
class InputBarController(
    val focusRequester: NativeFocusRequester,
    private val keyboardController: SoftwareKeyboardController?,
    private val recentEmojisStore: RecentEmojisStore,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(InputBarState())
    val state = _state.asStateFlow()

    // ============================================
    // 文本相关
    // ============================================

    /**
     * 更新输入文本
     */
    fun updateText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    /**
     * 清空输入
     */
    fun clearInput() {
        _state.update { it.copy(inputText = "") }
    }

    /**
     * 插入表情
     */
    fun insertEmoji(description: String) {
        val insertText = "[$description]"
        val cursorIndex = focusRequester.selectionStart
        val newText = StringBuilder(getCurrentText())
            .insert(cursorIndex, insertText).toString()
        updateText(newText)

        // 计算新的光标位置
        val newCursorIndex = cursorIndex + insertText.length
        scope.launch {
            delay(16)
            focusRequester.setSelection(newCursorIndex)
        }

        // 记录使用
        scope.launch {
            recentEmojisStore.record(description)
        }
    }

    /**
     * 处理退格（删除表情）
     */
    fun handleEmojiBackspace() {
        handleTextBackspace(
            getCurrentText(),
            focusRequester.selectionStart
        ) { newString, newPos ->
            updateText(newString)

            // 同步光标
            focusRequester.post {
                focusRequester.setSelection(newPos)
            }
        }
    }

    /**
     * 处理文本删除
     */
    fun handleTextBackspace(
        text: String,
        selectionStart: Int,
        onChange: (newText: String, newCursorPos: Int) -> Unit
    ) {
        if (selectionStart <= 0) return

        val textBefore = text.take(selectionStart)
        val textAfter = text.substring(selectionStart)

        // 匹配光标左侧紧邻的 "[xxx]"
        val match = emojiBackspacePatternRegex.find(textBefore)

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

    private val emojiBackspacePatternRegex =
        Regex("\\[[^\\[\\]]+]$") // 以 [ 开头，中间包含非括号字符，以 ] 结尾，且必须紧贴末尾($)

    private fun getCurrentText(): String {
        return _state.value.inputText
    }

    /**
     * 更新行数
     */
    fun updateLineCount(count: Int) {
        _state.update { it.copy(lineCount = count) }
    }

    /**
     * 加载最近使用的表情
     */
    private fun loadRecentEmojis() {
        scope.launch {
            val keys = recentEmojisStore.getRecentEmojis()
            val emojis = keys.mapNotNull { key ->
                Emojis.all.find { it.description == key }
            }
            _state.update { it.copy(recentEmojis = emojis) }
        }
    }

    // ============================================
    // 模式切换相关
    // ============================================

    /**
     * 切换输入模式
     *
     * @param target 目标模式
     * @param showKeyboard 是否显示键盘（仅对TEXT模式生效）
     */
    fun switchMode(target: InputMode = InputMode.Text, showKeyboard: Boolean = true) {
        _state.update { it.copy(inputMode = target) }

        when {
            // 切换到文本模式
            target.isText && showKeyboard -> {
                focusRequester.requestFocus()
            }
            // 切换到其他模式
            else -> {
                focusRequester.clearFocus()
                keyboardController?.hide()

                // 输入表情时显示光标
                if (target.isEmoji) {
                    scope.launch {
                        delay(200)
                        focusRequester.requestFocus(showKeyboard = false)
                    }

                    // 加载最近使用的表情
                    loadRecentEmojis()
                }
            }
        }
    }

    /**
     * 切换到文本模式并显示键盘
     */
    fun switchToTextMode() {
        switchMode(InputMode.Text, showKeyboard = true)
    }

    /**
     * 关闭所有面板和键盘
     */
    fun dismissAll() {
        switchMode(InputMode.Text, showKeyboard = false)
    }

    /**
     * 仅同步模式状态，不触发交互
     *
     * 内部使用，用于响应系统键盘状态变化
     */
    internal fun syncMode(target: InputMode) {
        _state.update { it.copy(inputMode = target) }
    }

    // ============================================
    // 其他状态管理
    // ============================================

    /**
     * 切换全屏输入
     */
    fun toggleExpand() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }
}

/**
 * 包含了完整的键盘管理、返回键处理等逻辑
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberInputBarController(
    focusRequester: NativeFocusRequester,
    recentEmojisStore: RecentEmojisStore = hiltViewModel<RecentEmojisViewModel>().store
): InputBarController {
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    // 创建控制器
    val controller = remember(focusRequester) {
        InputBarController(
            focusRequester = focusRequester,
            keyboardController = keyboardController,
            recentEmojisStore = recentEmojisStore,
            scope = scope
        )
    }
    val state by controller.state.collectAsState()

    // 监听系统键盘状态
    // 当键盘弹出时，自动切换到文本模式
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            controller.syncMode(InputMode.Text)
        }
    }

    // 返回键处理
    // 当面板展开时，返回键关闭面板而不是退出页面
    BackHandler(enabled = state.inputMode.isPanelMode) {
        controller.dismissAll()
    }

    return controller
}