package top.chengdongqing.wechat.core.designsystem.components.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.chengdongqing.wechat.core.designsystem.theme.White

/**
 * 加载中对话框组件
 *
 * @param isLoading 是否显示加载状态
 * @param size 加载指示器大小
 * @param color 加载指示器颜色
 */
@Composable
fun LoadingDialog(
    isLoading: Boolean,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    color: Color = White
) {
    if (isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WeLoading(size = size, color = color)
            }
        }
    }
}
