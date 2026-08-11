package top.chengdongqing.wechat.core.designsystem.compose

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 允许 Dp 类型在 Activity 重建或进程杀掉后恢复状态
 */
val DpSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp }
)
