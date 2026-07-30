package top.chengdongqing.wechat.feature.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.chengdongqing.wechat.core.designsystem.components.pin.PinEntry
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun AppUnlockScreen(
    onUnlocked: () -> Unit,
    verify: (String) -> Boolean,
    isTemporarilyLocked: () -> Boolean
) {
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        PinEntry(
            title = "微信已锁定",
            error = error,
            onPinComplete = {
                if (verify(it)) {
                    onUnlocked()
                } else {
                    error = if (isTemporarilyLocked()) {
                        "尝试次数过多，请稍后重试"
                    } else {
                        "密码错误"
                    }
                }
            }
        )
    }
}
