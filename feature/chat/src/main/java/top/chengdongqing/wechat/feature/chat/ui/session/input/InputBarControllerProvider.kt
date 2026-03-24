package top.chengdongqing.wechat.feature.chat.ui.session.input

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.designsystem.components.emojitextfield.NativeFocusRequester
import top.chengdongqing.wechat.feature.chat.data.store.RecentEmojisStore
import top.chengdongqing.wechat.feature.chat.domain.model.InputMode
import top.chengdongqing.wechat.feature.chat.ui.session.input.panel.RecentEmojisViewModel

/**
 * 创建并记住 [InputBarController]，包含完整的键盘监听和返回键处理逻辑。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberInputBarController(
    focusRequester: NativeFocusRequester,
    isSendButtonOn: Boolean = true,
    recentEmojisStore: RecentEmojisStore = hiltViewModel<RecentEmojisViewModel>().store
): InputBarController {
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    // 创建控制器
    val controller = remember(focusRequester, isSendButtonOn) {
        InputBarController(
            focusRequester = focusRequester,
            keyboardController = keyboardController,
            recentEmojisStore = recentEmojisStore,
            isSendButtonOn = isSendButtonOn,
            scope = scope
        )
    }
    val state by controller.state.collectAsState()

    // 监听系统键盘状态：键盘弹出时自动切换到文本模式
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            controller.syncMode(InputMode.Text)
        }
    }

    // 返回键处理：面板展开时拦截返回键，收起面板而不是退出页面
    BackHandler(enabled = state.inputMode.isPanelMode) {
        controller.dismissAll()
    }

    return controller
}
